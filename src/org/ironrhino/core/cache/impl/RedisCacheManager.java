package org.ironrhino.core.cache.impl;

import static org.ironrhino.core.metadata.Profiles.CLOUD;
import static org.ironrhino.core.metadata.Profiles.DUAL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.cache.CacheManager;
import org.ironrhino.core.spring.configuration.PrioritizedQualifier;
import org.ironrhino.core.spring.configuration.ServiceImplementationConditional;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

@SuppressWarnings({ "unchecked", "rawtypes" })
@Component("cacheManager")
@ServiceImplementationConditional(profiles = { DUAL, CLOUD })
public class RedisCacheManager implements CacheManager {

	@Autowired
	private Logger logger;

	@Autowired
	@PrioritizedQualifier("cacheRedisTemplate")
	private RedisTemplate redisTemplate;

	@Autowired
	@Qualifier("stringRedisTemplate")
	@PrioritizedQualifier("cacheStringRedisTemplate")
	private RedisTemplate<String, String> stringRedisTemplate;

	@Override
	public void put(String key, Object value, int timeToLive, TimeUnit timeUnit, String namespace) {
		put(key, value, -1, timeToLive, timeUnit, namespace);
	}

	@Override
	public void put(String key, Object value, int timeToIdle, int timeToLive, TimeUnit timeUnit, String namespace) {
		if (key == null || value == null)
			return;
		try {
			if (timeToLive > 0)
				redisTemplate.opsForValue().set(generateKey(key, namespace), value, timeToLive, timeUnit);
			else
				redisTemplate.opsForValue().set(generateKey(key, namespace), value);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public boolean exists(String key, String namespace) {
		if (key == null)
			return false;
		try {
			return redisTemplate.hasKey(generateKey(key, namespace));
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
			return redisTemplate.opsForValue().get(generateKey(key, namespace));
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
	public Object get(String key, String namespace, int timeToIdle, TimeUnit timeUnit) {
		if (key == null)
			return null;
		String actualKey = generateKey(key, namespace);
		if (timeToIdle > 0)
			redisTemplate.expire(actualKey, timeToIdle, timeUnit);
		try {
			return redisTemplate.opsForValue().get(actualKey);
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
	public void delete(String key, String namespace) {
		if (StringUtils.isBlank(key))
			return;
		try {
			redisTemplate.delete(generateKey(key, namespace));
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
				actualMap.put(redisTemplate.getKeySerializer().serialize(generateKey(entry.getKey(), namespace)),
						redisTemplate.getValueSerializer().serialize(entry.getValue()));
			redisTemplate.execute((RedisConnection conn) -> {
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
	public Map<String, Object> mget(Collection<String> keys, String namespace) {
		if (keys == null)
			return null;
		final List<byte[]> _keys = new ArrayList<>();
		for (String key : keys)
			_keys.add(redisTemplate.getKeySerializer().serialize(generateKey(key, namespace)));
		try {
			List<byte[]> values = (List<byte[]>) redisTemplate
					.execute((RedisConnection conn) -> conn.mGet(_keys.toArray(new byte[0][0])));
			Map<String, Object> map = new HashMap<>();
			int i = 0;
			for (String key : keys) {
				map.put(key, redisTemplate.getValueSerializer().deserialize(values.get(i)));
				i++;
			}
			return map;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return null;
		}
	}

	@Override
	public void mdelete(final Collection<String> keys, final String namespace) {
		if (keys == null)
			return;
		try {
			redisTemplate.execute((RedisConnection conn) -> {
				conn.multi();
				try {
					for (String key : keys)
						if (StringUtils.isNotBlank(key))
							conn.del(redisTemplate.getKeySerializer().serialize(generateKey(key, namespace)));
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
	public boolean containsKey(String key, String namespace) {
		if (key == null)
			return false;
		try {
			return redisTemplate.hasKey(generateKey(key, namespace));
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return false;
		}
	}

	@Override
	public boolean putIfAbsent(String key, Object value, int timeToLive, TimeUnit timeUnit, String namespace) {
		try {
			String actrualkey = generateKey(key, namespace);
			boolean success = redisTemplate.opsForValue().setIfAbsent(actrualkey, value);
			if (success && timeToLive > 0)
				redisTemplate.expire(actrualkey, timeToLive, timeUnit);
			return success;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public long increment(String key, long delta, int timeToLive, TimeUnit timeUnit, String namespace) {
		try {
			String actrualkey = generateKey(key, namespace);
			long result = redisTemplate.opsForValue().increment(actrualkey, delta);
			if (timeToLive > 0)
				redisTemplate.expire(actrualkey, timeToLive, timeUnit);
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
	public boolean supportsTimeToIdle() {
		return false;
	}

	@Override
	public boolean supportsUpdateTimeToLive() {
		return true;
	}

	@Override
	public void invalidate(String namespace) {
		RedisScript<Boolean> script = new DefaultRedisScript<>(
				"local keys = redis.call('keys', ARGV[1]) \n for i=1,#keys,5000 do \n redis.call('del', unpack(keys, i, math.min(i+4999, #keys))) \n end \n return true",
				Boolean.class);
		stringRedisTemplate.execute(script, null, namespace + ":*");
	}

}
