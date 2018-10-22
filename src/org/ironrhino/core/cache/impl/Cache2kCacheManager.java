package org.ironrhino.core.cache.impl;

import static org.ironrhino.core.metadata.Profiles.DEFAULT;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.lang3.StringUtils;
import org.cache2k.Cache;
import org.cache2k.CacheEntry;
import org.cache2k.configuration.Cache2kConfiguration;
import org.cache2k.expiry.ExpiryTimeValues;
import org.cache2k.processor.EntryProcessingException;
import org.ironrhino.core.cache.CacheManager;
import org.ironrhino.core.spring.configuration.ServiceImplementationConditional;
import org.springframework.stereotype.Component;

@Component("cacheManager")
@ServiceImplementationConditional(profiles = DEFAULT)
public class Cache2kCacheManager implements CacheManager {

	private org.cache2k.CacheManager cache2kCacheManager;

	@PostConstruct
	public void init() {
		cache2kCacheManager = org.cache2k.CacheManager.getInstance();
	}

	@PreDestroy
	public void destroy() {
		cache2kCacheManager.close();
	}

	@Override
	public void put(String key, Object value, int timeToLive, TimeUnit timeUnit, String namespace) {
		if (key == null || value == null)
			return;
		Cache<String, Object> cache = getCache(namespace, true);
		cache.invoke(key, e -> e.setValue(value).setExpiryTime(timeToLive == 0 ? ExpiryTimeValues.ETERNAL
				: (System.currentTimeMillis() + timeUnit.toMillis(timeToLive))));
	}

	@Override
	public void putWithTti(String key, Object value, int timeToIdle, TimeUnit timeUnit, String namespace) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean exists(String key, String namespace) {
		if (key == null)
			return false;
		Cache<String, Object> cache = getCache(namespace, false);
		if (cache == null)
			return false;
		return cache.containsKey(key);
	}

	@Override
	public Object get(String key, String namespace) {
		if (key == null)
			return null;
		Cache<String, Object> cache = getCache(namespace, false);
		if (cache == null)
			return null;
		return cache.get(key);
	}

	@Override
	public Object getWithTti(String key, String namespace, int timeToIdle, TimeUnit timeUnit) {
		if (key == null)
			return null;
		Cache<String, Object> cache = getCache(namespace, false);
		if (cache == null)
			return null;
		if (timeToIdle > 0)
			cache.expireAt(key, System.currentTimeMillis() + timeUnit.toMillis(timeToIdle));
		return get(key, namespace);
	}

	@Override
	public long ttl(String key, String namespace) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setTtl(String key, String namespace, int timeToLive, TimeUnit timeUnit) {
		Cache<String, Object> cache = getCache(namespace, false);
		if (cache == null || timeToLive <= 0)
			return;
		cache.expireAt(key, System.currentTimeMillis() + timeUnit.toMillis(timeToLive));
	}

	@Override
	public void delete(String key, String namespace) {
		if (key == null)
			return;
		Cache<String, Object> cache = getCache(namespace, false);
		if (cache != null)
			cache.remove(key);
	}

	@Override
	public void mput(Map<String, Object> map, int timeToLive, TimeUnit timeUnit, String namespace) {
		if (map == null)
			return;
		Cache<String, Object> cache = getCache(namespace, true);
		for (Map.Entry<String, Object> entry : map.entrySet())
			cache.invoke(entry.getKey(),
					e -> e.setValue(entry.getValue()).setExpiryTime(timeToLive == 0 ? ExpiryTimeValues.ETERNAL
							: (System.currentTimeMillis() + timeUnit.toMillis(timeToLive))));
	}

	@Override
	public Map<String, Object> mget(Set<String> keys, String namespace) {
		if (keys == null)
			return null;
		Cache<String, Object> cache = getCache(namespace, false);
		if (cache == null)
			return null;
		keys = keys.stream().filter(StringUtils::isNotBlank).collect(Collectors.toCollection(HashSet::new));
		return cache.getAll(keys);
	}

	@Override
	public void mdelete(Set<String> keys, String namespace) {
		if (keys == null)
			return;
		Cache<String, Object> cache = getCache(namespace, false);
		if (cache != null)
			cache.removeAll(
					keys.stream().filter(StringUtils::isNotBlank).collect(Collectors.toCollection(HashSet::new)));
	}

	@Override
	public boolean putIfAbsent(String key, Object value, int timeToLive, TimeUnit timeUnit, String namespace) {
		if (key == null || value == null)
			return false;
		Cache<String, Object> cache = getCache(namespace, true);
		boolean b = cache.putIfAbsent(key, value);
		if (b)
			cache.expireAt(key,
					System.currentTimeMillis() + (timeToLive > 0 ? timeUnit.toMillis(timeToLive) : Integer.MAX_VALUE));
		return b;
	}

	@Override
	public long increment(String key, long delta, int timeToLive, TimeUnit timeUnit, String namespace) {
		if (key == null)
			throw new IllegalArgumentException("key should not be null");
		if (delta == 0)
			throw new IllegalArgumentException("delta should not be 0");
		Cache<String, Object> cache = getCache(namespace, true);
		CacheEntry<String, Object> ce = cache.invoke(key, e -> {
			if (e.exists()) {
				e.setValue((Long) e.getValue() + delta);
				if (timeToLive > 0)
					e.setExpiryTime(System.currentTimeMillis() + timeUnit.toMillis(timeToLive));
			} else {
				e.setValue(delta);
				e.setExpiryTime(System.currentTimeMillis()
						+ (timeToLive > 0 ? timeUnit.toMillis(timeToLive) : Integer.MAX_VALUE));
			}
			return e;
		});
		return (Long) ce.getValue();
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
			CacheEntry<String, Object> ce = cache.invoke(key, e -> {
				if (e.exists()) {
					if ((Long) e.getValue() < delta)
						throw new IllegalStateException(
								"namespace:" + namespace + ", key:" + key + " is less than " + delta);
					e.setValue((Long) e.getValue() - delta);
					if (timeToLive > 0)
						e.setExpiryTime(System.currentTimeMillis() + timeUnit.toMillis(timeToLive));
				} else {
					throw new IllegalStateException("namespace:" + namespace + ", key:" + key + " does not exist");
				}
				return e;
			});
			return (Long) ce.getValue();
		} catch (EntryProcessingException e) {
			if (e.getCause() instanceof IllegalStateException)
				throw (IllegalStateException) e.getCause();
			throw e;
		}
	}

	@Override
	public boolean supportsTti() {
		return false;
	}

	@Override
	public boolean supportsGetTtl() {
		return false;
	}

	@Override
	public boolean supportsUpdateTtl() {
		return true;
	}

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
					Cache2kConfiguration<String, Object> cfg = Cache2kConfiguration.of(String.class, Object.class);
					cfg.setName(namespace);
					cfg.setExpireAfterWrite(3600 * 1000);
					cfg.setEntryCapacity(10000);
					cache = cache2kCacheManager.createCache(cfg);
				}
			}
		}
		return cache;
	}

}
