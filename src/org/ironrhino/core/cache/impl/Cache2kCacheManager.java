package org.ironrhino.core.cache.impl;

import static org.ironrhino.core.metadata.Profiles.DEFAULT;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.lang3.StringUtils;
import org.cache2k.Cache;
import org.cache2k.CacheEntry;
import org.cache2k.configuration.Cache2kConfiguration;
import org.cache2k.expiry.ExpiryTimeValues;
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
		cache.invoke(key, e -> e.setValue(value).setExpiry(timeToLive == 0 ? ExpiryTimeValues.ETERNAL
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
					e -> e.setValue(entry.getValue()).setExpiry(timeToLive == 0 ? ExpiryTimeValues.ETERNAL
							: (System.currentTimeMillis() + timeUnit.toMillis(timeToLive))));
	}

	@Override
	public Map<String, Object> mget(Set<String> keys, String namespace) {
		if (keys == null)
			return null;
		Cache<String, Object> cache = getCache(namespace, false);
		if (cache == null)
			return null;
		return cache.getAll(keys);
	}

	@Override
	public void mdelete(Set<String> keys, String namespace) {
		if (keys == null)
			return;
		Cache<String, Object> cache = getCache(namespace, false);
		if (cache != null)
			cache.removeAll(keys);
	}

	@Override
	public boolean putIfAbsent(String key, Object value, int timeToLive, TimeUnit timeUnit, String namespace) {
		if (key == null || value == null)
			return false;
		Cache<String, Object> cache = getCache(namespace, true);
		boolean b = cache.putIfAbsent(key, value);
		if (b)
			cache.expireAt(key, System.currentTimeMillis() + timeUnit.toMillis(timeToLive));
		return b;
	}

	@Override
	public long increment(String key, long delta, int timeToLive, TimeUnit timeUnit, String namespace) {
		if (key == null || delta == 0)
			return -1;
		Cache<String, Object> cache = getCache(namespace, true);
		CacheEntry<String, Object> ce = cache.invoke(key, e -> {
			if (e.exists()) {
				e.setValue((Long) e.getValue() + delta);
			} else {
				e.setValue(delta);
			}
			e.setExpiry(System.currentTimeMillis() + timeUnit.toMillis(timeToLive));
			return e;
		});
		return (Long) ce.getValue();
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
