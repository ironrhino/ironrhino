package org.ironrhino.core.cache.impl;

import static org.ironrhino.core.metadata.Profiles.DEFAULT;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.cache.CacheManager;
import org.ironrhino.core.spring.configuration.ServiceImplementationConditional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import lombok.Setter;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

@Component("cacheManager")
@ServiceImplementationConditional(profiles = DEFAULT)
public class EhCacheManager implements CacheManager {

	private net.sf.ehcache.CacheManager ehCacheManager;

	@Setter
	@Value("${ehcache.configLocation:classpath:ehcache.xml}")
	private Resource configLocation;

	@PostConstruct
	public void init() {
		try {
			ehCacheManager = net.sf.ehcache.CacheManager.create(configLocation.getInputStream());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@PreDestroy
	public void destroy() {
		ehCacheManager.shutdown();
	}

	@Override
	public void put(String key, Object value, int timeToLive, TimeUnit timeUnit, String namespace) {
		if (key == null || value == null)
			return;
		Cache cache = getCache(namespace, true);
		cache.put(new Element(key, value, timeToLive <= 0 ? true : null, null, (int) timeUnit.toSeconds(timeToLive)));
	}

	@Override
	public void putWithTti(String key, Object value, int timeToIdle, TimeUnit timeUnit, String namespace) {
		if (key == null || value == null)
			return;
		Cache cache = getCache(namespace, true);
		cache.put(new Element(key, value, null, (int) timeUnit.toSeconds(timeToIdle), null));
	}

	@Override
	public boolean exists(String key, String namespace) {
		if (key == null)
			return false;
		Cache cache = getCache(namespace, false);
		if (cache == null)
			return false;
		return cache.get(key) != null;
	}

	@Override
	public Object get(String key, String namespace) {
		if (key == null)
			return null;
		Cache cache = getCache(namespace, false);
		if (cache == null)
			return null;
		Element element = cache.get(key);
		return element != null ? element.getObjectValue() : null;
	}

	@Override
	public Object getWithTti(String key, String namespace, int timeToIdle, TimeUnit timeUnit) {
		if (key == null)
			return null;
		Cache cache = getCache(namespace, false);
		if (cache == null)
			return null;
		if (timeToIdle <= 0)
			return get(key, namespace);
		Element element = cache.get(key);
		if (element != null) {
			if (element.getTimeToIdle() != timeToIdle) {
				element.setTimeToIdle((int) timeUnit.toSeconds(timeToIdle));
				cache.put(element);
			}
			return element.getObjectValue();
		}
		return null;
	}

	@Override
	public long ttl(String key, String namespace) {
		if (key == null)
			return 0;
		Cache cache = getCache(namespace, false);
		if (cache == null)
			return 0;
		Element element = cache.get(key);
		if (element != null)
			return element.getExpirationTime() - System.currentTimeMillis();
		return 0;
	}

	@Override
	public void setTtl(String key, String namespace, int timeToLive, TimeUnit timeUnit) {
		Cache cache = getCache(namespace, false);
		if (cache == null)
			return;
		Element element = cache.get(key);
		if (element != null)
			cache.put(new Element(key, element.getObjectValue(), timeToLive <= 0 ? true : null, null,
					(int) timeUnit.toSeconds(timeToLive)));
	}

	@Override
	public void delete(String key, String namespace) {
		if (key == null)
			return;
		Cache cache = getCache(namespace, false);
		if (cache != null)
			cache.remove(key);
	}

	@Override
	public void mput(Map<String, Object> map, int timeToLive, TimeUnit timeUnit, String namespace) {
		if (map == null)
			return;
		Cache cache = getCache(namespace, true);
		for (Map.Entry<String, Object> entry : map.entrySet())
			cache.put(new Element(entry.getKey(), entry.getValue(), timeToLive <= 0 ? true : null, null,
					timeToLive > 0 ? (int) timeUnit.toSeconds(timeToLive) : null));
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<String, Object> mget(Set<String> keys, String namespace) {
		if (keys == null)
			return null;
		Cache cache = getCache(namespace, false);
		if (cache == null)
			return null;
		return cache.getAllWithLoader(keys, null);
	}

	@Override
	public void mdelete(Set<String> keys, String namespace) {
		if (keys == null)
			return;
		Cache cache = getCache(namespace, false);
		if (cache != null)
			for (String key : keys)
				if (StringUtils.isNotBlank(key))
					cache.remove(key);
	}

	@Override
	public boolean putIfAbsent(String key, Object value, int timeToLive, TimeUnit timeUnit, String namespace) {
		if (key == null || value == null)
			return false;
		Cache cache = getCache(namespace, true);
		return cache.putIfAbsent(new Element(key, value, timeToLive <= 0 ? true : null, null,
				(int) timeUnit.toSeconds(timeToLive))) == null;
	}

	@Override
	public long increment(String key, long delta, int timeToLive, TimeUnit timeUnit, String namespace) {
		if (key == null || delta == 0)
			return -1;
		Cache cache = getCache(namespace, true);
		Element element = cache.putIfAbsent(new Element(key, Long.valueOf(delta), timeToLive <= 0 ? true : null, null,
				(int) timeUnit.toSeconds(timeToLive)));
		if (element == null) {
			return delta;
		} else {
			synchronized (cache) {
				element = cache.get(key);
				if (element == null) {
					cache.put(new Element(key, Long.valueOf(delta), timeToLive <= 0 ? true : null, null,
							(int) timeUnit.toSeconds(timeToLive)));
					return delta;
				} else {
					long value = ((long) element.getObjectValue()) + delta;
					cache.put(new Element(key, Long.valueOf(value), timeToLive <= 0 ? true : null, null,
							(int) timeUnit.toSeconds(timeToLive)));
					return value;
				}
			}
		}
	}

	@Override
	public boolean supportsTti() {
		return true;
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
		Cache cache = ehCacheManager.getCache(namespace);
		if (cache != null) {
			cache.removeAll();
		}
	}

	private Cache getCache(String namespace, boolean create) {
		if (StringUtils.isBlank(namespace))
			namespace = "_default";
		Cache cache = ehCacheManager.getCache(namespace);
		if (cache != null)
			return cache;
		if (create) {
			synchronized (this) {
				cache = ehCacheManager.getCache(namespace);
				if (cache == null) {
					ehCacheManager.addCache(namespace);
					cache = ehCacheManager.getCache(namespace);
				}
			}
		}
		return cache;
	}
}
