package org.ironrhino.core.cache.impl;

import static org.ironrhino.core.metadata.Profiles.CLOUD;
import static org.ironrhino.core.metadata.Profiles.DUAL;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.cache.CacheManager;
import org.ironrhino.core.spring.configuration.PriorityQualifier;
import org.ironrhino.core.spring.configuration.ServiceImplementationConditional;
import org.ironrhino.core.spring.data.redis.FallbackToStringSerializer;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import lombok.extern.slf4j.Slf4j;

@SuppressWarnings({ "unchecked", "rawtypes" })
@Component("cacheManager")
@ServiceImplementationConditional(profiles = { DUAL, CLOUD })
@Slf4j
public class RedisCacheManager implements CacheManager {

	public static final String DEFAULT_SERIALIZER = "cacheManager.redis.defaultSerializer";

	public static final String SERIALIZERS_PREFIX = "cacheManager.redis.serializers.";

	@Autowired
	@PriorityQualifier
	private RedisTemplate cacheRedisTemplate;

	@Autowired
	@Qualifier("stringRedisTemplate")
	@PriorityQualifier
	private StringRedisTemplate cacheStringRedisTemplate;

	@Autowired
	private Environment env;

	private Map<String, RedisTemplate> cache = new ConcurrentHashMap<>();

	private RedisScript<Long> decrementPositiveScript = new DefaultRedisScript<>(
			"if redis.call('exists',KEYS[1])==1 then local v=redis.call('decrby',KEYS[1],ARGV[1]) if v >= 0 then return v else redis.call('incrby',KEYS[1],ARGV[1]) return -2 end else return -1 end",
			Long.class);

	@PostConstruct
	public void init() {
		String defaultSerializerClass = env.getProperty(DEFAULT_SERIALIZER);
		if (StringUtils.isNotBlank(defaultSerializerClass)) {
			try {
				cacheRedisTemplate.setValueSerializer((RedisSerializer) BeanUtils.instantiateClass(
						ClassUtils.forName(defaultSerializerClass, RedisCacheManager.class.getClassLoader())));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} else {
			cacheRedisTemplate.setValueSerializer(new FallbackToStringSerializer());
		}
	}

	@Override
	public void put(String key, Object value, int timeToLive, TimeUnit timeUnit, String namespace) {
		RedisTemplate redisTemplate = findRedisTemplate(namespace);
		try {
			if (timeToLive > 0)
				redisTemplate.opsForValue().set(generateKey(key, namespace), value, timeToLive, timeUnit);
			else
				redisTemplate.opsForValue().set(generateKey(key, namespace), value);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
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
			Boolean b = findRedisTemplate(namespace).hasKey(generateKey(key, namespace));
			return b != null && b;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return false;
		}
	}

	@Override
	public Object get(String key, String namespace) {
		if (key == null)
			return null;
		try {
			return findRedisTemplate(namespace).opsForValue().get(generateKey(key, namespace));
		} catch (SerializationException e) {
			log.warn(e.getMessage());
			delete(key, namespace);
			return null;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}

	@Override
	public Object getWithTti(String key, String namespace, int timeToIdle, TimeUnit timeUnit) {
		if (key == null)
			return null;
		String actualKey = generateKey(key, namespace);
		RedisTemplate redisTemplate = findRedisTemplate(namespace);
		try {
			if (timeToIdle > 0)
				redisTemplate.expire(actualKey, timeToIdle, timeUnit);
			return redisTemplate.opsForValue().get(actualKey);
		} catch (SerializationException e) {
			log.warn(e.getMessage());
			delete(key, namespace);
			return null;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}

	@Override
	public long ttl(String key, String namespace) {
		if (key == null)
			return 0;
		String actualKey = generateKey(key, namespace);
		Long value = findRedisTemplate(namespace).getExpire(actualKey, TimeUnit.MILLISECONDS);
		if (value == null)
			value = 0L;
		if (value == -2)
			value = 0L; // not exists
		return value;
	}

	@Override
	public void setTtl(String key, String namespace, int timeToLive, TimeUnit timeUnit) {
		if (key == null)
			return;
		findRedisTemplate(namespace).expire(generateKey(key, namespace), timeToLive, timeUnit);
	}

	@Override
	public void delete(String key, String namespace) {
		if (StringUtils.isBlank(key))
			return;
		try {
			findRedisTemplate(namespace).delete(generateKey(key, namespace));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	@Override
	public void mput(Map<String, Object> map, final int timeToLive, TimeUnit timeUnit, String namespace) {
		if (map == null)
			return;
		RedisTemplate redisTemplate = findRedisTemplate(namespace);
		try {
			Map<String, Object> temp = new HashMap<>();
			map.forEach((key, value) -> temp.put(generateKey(key, namespace), value));
			redisTemplate.opsForValue().multiSet(temp);
			temp.keySet().forEach(key -> redisTemplate.expire(key, timeToLive, timeUnit));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	@Override
	public Map<String, Object> mget(Set<String> keys, String namespace) {
		if (keys == null)
			return null;
		keys = keys.stream().filter(StringUtils::isNotBlank).collect(Collectors.toCollection(HashSet::new));
		try {
			List<Object> list = findRedisTemplate(namespace).opsForValue()
					.multiGet(keys.stream().map(key -> generateKey(key, namespace)).collect(Collectors.toList()));
			Map<String, Object> result = new HashMap<>();
			int i = 0;
			for (String key : keys) {
				result.put(key, list.get(i));
				i++;
			}
			return result;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}

	@Override
	public void mdelete(Set<String> keys, final String namespace) {
		if (keys == null)
			return;
		try {
			findRedisTemplate(namespace).delete(keys.stream().filter(StringUtils::isNotBlank)
					.map(key -> generateKey(key, namespace)).collect(Collectors.toList()));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	@Override
	public boolean putIfAbsent(String key, Object value, int timeToLive, TimeUnit timeUnit, String namespace) {
		String actualkey = generateKey(key, namespace);
		RedisTemplate redisTemplate = findRedisTemplate(namespace);
		Boolean success = redisTemplate.opsForValue().setIfAbsent(actualkey, value);
		if (success == null)
			return false;
		if (success && timeToLive > 0)
			redisTemplate.expire(actualkey, timeToLive, timeUnit);
		return success;
	}

	@Override
	public long increment(String key, long delta, int timeToLive, TimeUnit timeUnit, String namespace) {
		String actualkey = generateKey(key, namespace);
		RedisTemplate redisTemplate = findRedisTemplate(namespace);
		Long result = redisTemplate.opsForValue().increment(actualkey, delta);
		if (result == null)
			throw new RuntimeException("Unexpected null");
		if (timeToLive > 0)
			redisTemplate.expire(actualkey, timeToLive, timeUnit);
		return result;
	}

	@Override
	public long decrementAndReturnNonnegative(String key, long delta, int timeToLive, TimeUnit timeUnit,
			String namespace) {
		if (delta <= 0)
			throw new IllegalArgumentException("delta should great than 0");
		RedisTemplate redisTemplate = findRedisTemplate(namespace);
		String actualkey = generateKey(key, namespace);
		Long result = (Long) redisTemplate.execute(decrementPositiveScript, redisTemplate.getStringSerializer(),
				redisTemplate.getValueSerializer(), Collections.singletonList(actualkey), String.valueOf(delta));
		if (result == null)
			throw new RuntimeException("Unexpected null");
		if (result == -1)
			throw new IllegalStateException("namespace:" + namespace + ", key:" + key + " does not exist");
		if (result == -2)
			throw new IllegalStateException("namespace:" + namespace + ", key:" + key + " is less than " + delta);
		if (timeToLive > 0)
			redisTemplate.expire(actualkey, timeToLive, timeUnit);
		return result;
	}

	private String generateKey(String key, String namespace) {
		if (key == null)
			throw new IllegalArgumentException("key should not be null");
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
		cacheStringRedisTemplate.execute(script, Collections.emptyList(), namespace + ":*");
	}

	protected RedisTemplate findRedisTemplate(String namespace) {
		if (StringUtils.isBlank(namespace))
			return cacheRedisTemplate;
		String serializerClass = env.getProperty(SERIALIZERS_PREFIX + namespace);
		if (StringUtils.isBlank(serializerClass))
			return cacheRedisTemplate;
		return cache.computeIfAbsent(namespace, key -> {
			RedisTemplate rt = new RedisTemplate();
			BeanUtils.copyProperties(cacheRedisTemplate, rt);
			try {
				rt.setValueSerializer((RedisSerializer) BeanUtils.instantiateClass(
						ClassUtils.forName(serializerClass, RedisCacheManager.class.getClassLoader())));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			rt.afterPropertiesSet();
			return rt;
		});
	}

}
