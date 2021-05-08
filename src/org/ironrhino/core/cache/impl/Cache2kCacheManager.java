package org.ironrhino.core.cache.impl;

import static org.ironrhino.core.metadata.Profiles.DEFAULT;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.lang3.StringUtils;
import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.cache2k.expiry.ExpiryTimeValues;
import org.cache2k.processor.EntryProcessingException;
import org.ironrhino.core.cache.CacheManager;
import org.ironrhino.core.spring.configuration.ServiceImplementationConditional;
import org.springframework.stereotype.Component;

@Component("cacheManager")
@ServiceImplementationConditional(profiles = DEFAULT)
public class Cache2kCacheManager implements CacheManager {

	private static final AtomicInteger INSTANCE_NUMBER = new AtomicInteger();

	private org.cache2k.CacheManager cache2kCacheManager;

	@PostConstruct
	public void init() {
		int number = INSTANCE_NUMBER.getAndIncrement();
		String name = number > 0 ? "ironrhino" + number : "ironrhino";
		cache2kCacheManager = org.cache2k.CacheManager.getInstance(name);
	}

	@PreDestroy
	public void destroy() {
		cache2kCacheManager.close();
	}

	@Override
	public void put(String key, Object value, int timeToLive, TimeUnit timeUnit, String namespace) {
		if (key == null)
			throw new IllegalArgumentException("key should not be null");
		if (value == null)
			throw new IllegalArgumentException("value should not be null");
		Cache<String, Object> cache = getCache(namespace, true);
		cache.invoke(key, entry -> entry.setValue(value).setExpiryTime(timeToLive <= 0 ? ExpiryTimeValues.ETERNAL
				: (System.currentTimeMillis() + timeUnit.toMillis(timeToLive))));
	}

	@Override
	public boolean exists(String key, String namespace) {
		if (key == null)
			throw new IllegalArgumentException("key should not be null");
		Cache<String, Object> cache = getCache(namespace, false);
		if (cache == null)
			return false;
		return cache.containsKey(key);
	}

	@Override
	public Object get(String key, String namespace) {
		if (key == null)
			throw new IllegalArgumentException("key should not be null");
		Cache<String, Object> cache = getCache(namespace, false);
		if (cache == null)
			return null;
		return cache.get(key);
	}

	@Override
	public Object getWithTti(String key, String namespace, int timeToIdle, TimeUnit timeUnit) {
		if (key == null)
			throw new IllegalArgumentException("key should not be null");
		Cache<String, Object> cache = getCache(namespace, false);
		if (cache == null)
			return null;
		if (timeToIdle > 0) {
			return cache.invoke(key, entry -> entry
					.setExpiryTime(System.currentTimeMillis() + timeUnit.toMillis(timeToIdle)).getValue());
		}
		return get(key, namespace);
	}

	@Override
	public long ttl(String key, String namespace) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setTtl(String key, String namespace, int timeToLive, TimeUnit timeUnit) {
		if (key == null)
			throw new IllegalArgumentException("key should not be null");
		if (timeToLive <= 0)
			throw new IllegalArgumentException("timeToLive should be postive");
		Cache<String, Object> cache = getCache(namespace, false);
		if (cache != null)
			cache.invoke(key, entry -> entry.setExpiryTime(System.currentTimeMillis() + timeUnit.toMillis(timeToLive)));
	}

	@Override
	public void delete(String key, String namespace) {
		if (key == null)
			throw new IllegalArgumentException("key should not be null");
		Cache<String, Object> cache = getCache(namespace, false);
		if (cache != null)
			cache.remove(key);
	}

	@Override
	public void mput(Map<String, Object> map, int timeToLive, TimeUnit timeUnit, String namespace) {
		if (map == null)
			throw new IllegalArgumentException("map should not be null");
		Cache<String, Object> cache = getCache(namespace, true);
		map.forEach((k, v) -> {
			cache.invoke(k, entry -> entry.setValue(v).setExpiryTime(timeToLive <= 0 ? ExpiryTimeValues.ETERNAL
					: (System.currentTimeMillis() + timeUnit.toMillis(timeToLive))));
		});
	}

	@Override
	public Map<String, Object> mget(Collection<String> keys, String namespace) {
		if (keys == null)
			throw new IllegalArgumentException("keys should not be null");
		Cache<String, Object> cache = getCache(namespace, false);
		if (cache == null)
			return Collections.emptyMap();
		keys = keys.stream().filter(StringUtils::isNotBlank).collect(Collectors.toCollection(HashSet::new));
		return cache.getAll(keys);
	}

	@Override
	public void mdelete(Collection<String> keys, String namespace) {
		if (keys == null)
			throw new IllegalArgumentException("keys should not be null");
		Cache<String, Object> cache = getCache(namespace, false);
		if (cache != null)
			cache.removeAll(
					keys.stream().filter(StringUtils::isNotBlank).collect(Collectors.toCollection(HashSet::new)));
	}

	@Override
	public boolean putIfAbsent(String key, Object value, int timeToLive, TimeUnit timeUnit, String namespace) {
		if (key == null)
			throw new IllegalArgumentException("key should not be null");
		if (value == null)
			throw new IllegalArgumentException("value should not be null");
		Cache<String, Object> cache = getCache(namespace, true);
		boolean b = cache.putIfAbsent(key, value);
		if (b)
			cache.invoke(key, entry -> entry.setExpiryTime(
					System.currentTimeMillis() + (timeToLive > 0 ? timeUnit.toMillis(timeToLive) : Integer.MAX_VALUE)));
		return b;
	}

	@Override
	public long increment(String key, long delta, int timeToLive, TimeUnit timeUnit, String namespace) {
		if (key == null)
			throw new IllegalArgumentException("key should not be null");
		Cache<String, Object> cache = getCache(namespace, true);
		return cache.invoke(key, entry -> {
			Long newValue;
			if (entry.exists()) {
				newValue = (Long) entry.getValue() + delta;
				if (timeToLive > 0)
					entry.setExpiryTime(System.currentTimeMillis() + timeUnit.toMillis(timeToLive));
			} else {
				newValue = delta;
				entry.setExpiryTime(System.currentTimeMillis()
						+ (timeToLive > 0 ? timeUnit.toMillis(timeToLive) : Integer.MAX_VALUE));
			}
			entry.setValue(newValue);
			return newValue;
		});
	}

	@Override
	public long decrementAndReturnNonnegative(String key, long delta, int timeToLive, TimeUnit timeUnit,
			String namespace) {
		if (key == null)
			throw new IllegalArgumentException("key should not be null");
		if (delta <= 0)
			throw new IllegalArgumentException("delta should great than 0");
		Cache<String, Object> cache = getCache(namespace, true);
		try {
			return cache.invoke(key, entry -> {
				if (entry.exists()) {
					if ((Long) entry.getValue() < delta)
						throw new IllegalStateException(
								"namespace:" + namespace + ", key:" + key + " is less than " + delta);
					Long newValue = (Long) entry.getValue() - delta;
					entry.setValue(newValue);
					if (timeToLive > 0)
						entry.setExpiryTime(System.currentTimeMillis() + timeUnit.toMillis(timeToLive));
					return newValue;
				} else {
					throw new IllegalStateException("namespace:" + namespace + ", key:" + key + " does not exist");
				}
			});
		} catch (EntryProcessingException e) {
			if (e.getCause() instanceof IllegalStateException)
				throw (IllegalStateException) e.getCause();
			throw e;
		}
	}

	@Override
	public boolean supportsGetTtl() {
		return false;
	}

	@Override
	public boolean supportsUpdateTtl() {
		return true;
	}

	@Override
	public void invalidate(String namespace) {
		Cache<String, Object> cache = cache2kCacheManager.getCache(namespace);
		if (cache != null) {
			cache.clear();
		}
	}

	private Cache<String, Object> getCache(String namespace, boolean create) {
		if (StringUtils.isBlank(namespace))
			namespace = "_default";
		namespace = namespace.replaceAll(":", ".");
		Cache<String, Object> cache = cache2kCacheManager.getCache(namespace);
		if (cache != null)
			return cache;
		if (create) {
			synchronized (this) {
				cache = cache2kCacheManager.getCache(namespace);
				if (cache == null) {
					cache = cache2kCacheManager
							.createCache(Cache2kBuilder.of(String.class, Object.class).name(namespace)
									.expireAfterWrite(1, TimeUnit.HOURS).entryCapacity(10000).toConfiguration());
				}
			}
		}
		return cache;
	}

}
