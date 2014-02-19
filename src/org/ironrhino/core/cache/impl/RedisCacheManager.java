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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@SuppressWarnings({ "unchecked", "rawtypes" })
@Component("cacheManager")
@Profile({ DUAL, CLOUD })
public class RedisCacheManager implements CacheManager {

	private Logger log = LoggerFactory.getLogger(getClass());

	private RedisTemplate redisTemplate;

	public void setRedisTemplate(RedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	@Override
	public void put(String key, Object value, int timeToLive,
			TimeUnit timeUnit, String namespace) {
		put(key, value, -1, timeToLive, timeUnit, namespace);
	}

	@Override
	public void put(String key, Object value, int timeToIdle, int timeToLive,
			TimeUnit timeUnit, String namespace) {
		if (key == null || value == null)
			return;
		try {
			if (timeToLive > 0)
				redisTemplate.opsForValue().set(generateKey(key, namespace),
						value, timeToLive, timeUnit);
			else
				redisTemplate.opsForValue().set(generateKey(key, namespace),
						value);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	@Override
	public Object get(String key, String namespace) {
		if (key == null)
			return null;
		try {
			return redisTemplate.opsForValue().get(generateKey(key, namespace));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}

	@Override
	public Object get(String key, String namespace, int timeToLive,
			TimeUnit timeUnit) {
		if (key == null)
			return null;
		String actualKey = generateKey(key, namespace);
		if (timeToLive > 0)
			redisTemplate.expire(actualKey, timeToLive, timeUnit);
		return redisTemplate.opsForValue().get(actualKey);
	}

	@Override
	public void delete(String key, String namespace) {
		if (key == null)
			return;
		try {
			redisTemplate.delete(generateKey(key, namespace));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	@Override
	public void mput(Map<String, Object> map, final int timeToLive,
			TimeUnit timeUnit, String namespace) {
		if (map == null)
			return;
		try {
			final Map<byte[], byte[]> actualMap = new HashMap<byte[], byte[]>();
			for (Map.Entry<String, Object> entry : map.entrySet())
				actualMap.put(
						redisTemplate.getKeySerializer().serialize(
								generateKey(entry.getKey(), namespace)),
						redisTemplate.getValueSerializer().serialize(
								entry.getValue()));
			redisTemplate.execute(new RedisCallback<Object>() {
				@Override
				public Object doInRedis(RedisConnection conn)
						throws DataAccessException {
					conn.multi();
					conn.mSet(actualMap);
					if (timeToLive > 0)
						for (byte[] k : actualMap.keySet())
							conn.expire(k, timeToLive);
					conn.exec();
					return null;
				}
			});

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	@Override
	public Map<String, Object> mget(Collection<String> keys, String namespace) {
		if (keys == null)
			return null;
		final List<byte[]> _keys = new ArrayList<byte[]>();
		for (String key : keys)
			_keys.add(redisTemplate.getKeySerializer().serialize(
					generateKey(key, namespace)));
		try {
			List<byte[]> values = (List<byte[]>) redisTemplate
					.execute(new RedisCallback<List<byte[]>>() {
						@Override
						public List<byte[]> doInRedis(RedisConnection conn)
								throws DataAccessException {
							return conn.mGet(_keys.toArray(new byte[0][0]));
						}
					});
			Map<String, Object> map = new HashMap<String, Object>();
			int i = 0;
			for (String key : keys) {
				map.put(key,
						redisTemplate.getValueSerializer().deserialize(
								values.get(i)));
				i++;
			}
			return map;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}

	@Override
	public void mdelete(final Collection<String> keys, String namespace) {
		if (keys == null)
			return;
		final String ns = namespace;
		try {
			redisTemplate.execute(new RedisCallback() {
				@Override
				public Object doInRedis(RedisConnection conn)
						throws DataAccessException {
					conn.multi();
					for (String key : keys)
						if (key != null)
							conn.del(redisTemplate.getKeySerializer()
									.serialize(generateKey(key, ns)));
					conn.exec();
					return null;
				}
			});
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		for (String key : keys)
			delete(key, namespace);
	}

	@Override
	public boolean containsKey(String key, String namespace) {
		if (key == null)
			return false;
		try {
			return redisTemplate.hasKey(generateKey(key, namespace));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return false;
		}
	}

	@Override
	public boolean putIfAbsent(String key, Object value, int timeToLive,
			TimeUnit timeUnit, String namespace) {
		try {
			String actrualkey = generateKey(key, namespace);
			boolean success = redisTemplate.opsForValue().setIfAbsent(
					actrualkey, value);
			if (success && timeToLive > 0)
				redisTemplate.expire(actrualkey, timeToLive, timeUnit);
			return success;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public long increment(String key, long delta, int timeToLive,
			TimeUnit timeUnit, String namespace) {
		try {
			String actrualkey = generateKey(key, namespace);
			long result = redisTemplate.opsForValue().increment(actrualkey,
					delta);
			if (timeToLive > 0)
				redisTemplate.expire(actrualkey, timeToLive, timeUnit);
			return result;
		} catch (Exception e) {
			return -1;
		}
	}

	private String generateKey(String key, String namespace) {
		if (StringUtils.isNotBlank(namespace)) {
			StringBuilder sb = new StringBuilder(namespace.length()
					+ key.length() + 1);
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

}
