package org.ironrhino.core.cache.impl;

import static org.ironrhino.core.metadata.Profiles.CLOUD;
import static org.ironrhino.core.metadata.Profiles.DUAL;

import java.io.StreamCorruptedException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.cache.CacheManager;
import org.ironrhino.core.spring.configuration.PriorityQualifier;
import org.ironrhino.core.spring.configuration.ServiceImplementationConditional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Component;

@SuppressWarnings({ "unchecked", "rawtypes" })
@Component("cacheManager")
@ServiceImplementationConditional(profiles = { DUAL, CLOUD })
public class RedisCacheManager implements CacheManager {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	@PriorityQualifier
	private RedisTemplate cacheRedisTemplate;

	@Autowired
	@Qualifier("stringRedisTemplate")
	@PriorityQualifier
	private StringRedisTemplate cacheStringRedisTemplate;

	@PostConstruct
	public void init() {
		cacheRedisTemplate.setValueSerializer(new FallbackToStringSerializer());
	}

	@Override
	public void put(String key, Object value, int timeToLive, TimeUnit timeUnit, String namespace) {
		try {
			if (timeToLive > 0)
				cacheRedisTemplate.opsForValue().set(generateKey(key, namespace), value, timeToLive, timeUnit);
			else
				cacheRedisTemplate.opsForValue().set(generateKey(key, namespace), value);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public void putWithTti(String key, Object value, int timeToIdle, TimeUnit timeUnit, String namespace) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean exists(String key, String namespace) {
		if (key == null)
			return false;
		try {
			return cacheRedisTemplate.hasKey(generateKey(key, namespace));
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return false;
		}
	}

	@Override
	public Object get(String key, String namespace) {
		if (key == null)
			return null;
		try {
			return cacheRedisTemplate.opsForValue().get(generateKey(key, namespace));
		} catch (SerializationFailedException e) {
			logger.warn(e.getMessage(), e);
			delete(key, namespace);
			return null;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return null;
		}
	}

	@Override
	public Object getWithTti(String key, String namespace, int timeToIdle, TimeUnit timeUnit) {
		if (key == null)
			return null;
		String actualKey = generateKey(key, namespace);
		try {
			if (timeToIdle > 0)
				cacheRedisTemplate.expire(actualKey, timeToIdle, timeUnit);
			return cacheRedisTemplate.opsForValue().get(actualKey);
		} catch (SerializationFailedException e) {
			logger.warn(e.getMessage(), e);
			delete(key, namespace);
			return null;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return null;
		}
	}

	@Override
	public long ttl(String key, String namespace) {
		if (key == null)
			return 0;
		String actualKey = generateKey(key, namespace);
		long value = cacheRedisTemplate.getExpire(actualKey, TimeUnit.MILLISECONDS);
		if (value == -2)
			value = 0; // not exists
		return value;
	}

	@Override
	public void setTtl(String key, String namespace, int timeToLive, TimeUnit timeUnit) {
		if (key == null)
			return;
		cacheRedisTemplate.expire(generateKey(key, namespace), timeToLive, timeUnit);
	}

	@Override
	public void delete(String key, String namespace) {
		if (StringUtils.isBlank(key))
			return;
		try {
			cacheRedisTemplate.delete(generateKey(key, namespace));
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public void mput(Map<String, Object> map, final int timeToLive, TimeUnit timeUnit, String namespace) {
		if (map == null)
			return;
		try {
			final Map<byte[], byte[]> actualMap = new HashMap<>();
			for (Map.Entry<String, Object> entry : map.entrySet())
				actualMap.put(cacheRedisTemplate.getKeySerializer().serialize(generateKey(entry.getKey(), namespace)),
						cacheRedisTemplate.getValueSerializer().serialize(entry.getValue()));
			cacheRedisTemplate.execute((RedisConnection conn) -> {
				conn.multi();
				try {
					conn.mSet(actualMap);
					if (timeToLive > 0)
						for (byte[] k : actualMap.keySet())
							conn.expire(k, timeToLive);
					conn.exec();
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
					conn.discard();
				}
				return null;
			});

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public Map<String, Object> mget(Set<String> keys, String namespace) {
		if (keys == null)
			return null;
		final List<byte[]> _keys = new ArrayList<>();
		for (String key : keys)
			_keys.add(cacheRedisTemplate.getKeySerializer().serialize(generateKey(key, namespace)));
		try {
			List<byte[]> values = (List<byte[]>) cacheRedisTemplate
					.execute((RedisConnection conn) -> conn.mGet(_keys.toArray(new byte[0][0])));
			Map<String, Object> map = new HashMap<>();
			int i = 0;
			for (String key : keys) {
				map.put(key, cacheRedisTemplate.getValueSerializer().deserialize(values.get(i)));
				i++;
			}
			return map;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return null;
		}
	}

	@Override
	public void mdelete(final Set<String> keys, final String namespace) {
		if (keys == null)
			return;
		try {
			cacheRedisTemplate.execute((RedisConnection conn) -> {
				conn.multi();
				try {
					for (String key : keys)
						if (StringUtils.isNotBlank(key))
							conn.del(cacheRedisTemplate.getKeySerializer().serialize(generateKey(key, namespace)));
					conn.exec();
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
					conn.discard();
				}
				return null;
			});
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public boolean putIfAbsent(String key, Object value, int timeToLive, TimeUnit timeUnit, String namespace) {
		try {
			String actualkey = generateKey(key, namespace);
			boolean success = cacheRedisTemplate.opsForValue().setIfAbsent(actualkey, value);
			if (success && timeToLive > 0)
				cacheRedisTemplate.expire(actualkey, timeToLive, timeUnit);
			return success;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public long increment(String key, long delta, int timeToLive, TimeUnit timeUnit, String namespace) {
		try {
			String actualkey = generateKey(key, namespace);
			long result = cacheRedisTemplate.opsForValue().increment(actualkey, delta);
			if (timeToLive > 0)
				cacheRedisTemplate.expire(actualkey, timeToLive, timeUnit);
			return result;
		} catch (Exception e) {
			return -1;
		}
	}

	private String generateKey(String key, String namespace) {
		if (StringUtils.isNotBlank(namespace)) {
			StringBuilder sb = new StringBuilder(namespace.length() + key.length() + 1);
			sb.append(namespace);
			sb.append(':');
			sb.append(key);
			return sb.toString();
		} else {
			return key;
		}

	}

	@Override
	public boolean supportsTti() {
		return false;
	}

	@Override
	public boolean supportsGetTtl() {
		return true;
	}

	@Override
	public boolean supportsUpdateTtl() {
		return true;
	}

	public void invalidate(String namespace) {
		RedisScript<Boolean> script = new DefaultRedisScript<>(
				"local keys = redis.call('keys', ARGV[1]) \n for i=1,#keys,5000 do \n redis.call('del', unpack(keys, i, math.min(i+4999, #keys))) \n end \n return true",
				Boolean.class);
		cacheStringRedisTemplate.execute(script, null, namespace + ":*");
	}

	private static class FallbackToStringSerializer extends JdkSerializationRedisSerializer {

		@Override
		public byte[] serialize(Object object) {
			if (object instanceof String)
				return ((String) object).getBytes(StandardCharsets.UTF_8);
			return super.serialize(object);
		}

		@Override
		public Object deserialize(byte[] bytes) {
			try {
				return super.deserialize(bytes);
			} catch (SerializationException se) {
				if (se.getCause() instanceof SerializationFailedException
						&& se.getCause().getCause() instanceof StreamCorruptedException
						&& org.ironrhino.core.util.StringUtils.isUtf8(bytes))
					return new String(bytes, StandardCharsets.UTF_8);
				throw se;
			}
		}

	}

}
