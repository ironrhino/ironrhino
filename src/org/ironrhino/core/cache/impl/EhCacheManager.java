package org.ironrhino.core.cache.impl;

import static org.ironrhino.core.metadata.Profiles.DEFAULT;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.cache.CacheManager;
import org.ironrhino.core.spring.configuration.ServiceImplementationConditional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.google.common.util.concurrent.Striped;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.ObjectExistsException;

@Component("cacheManager")
@ServiceImplementationConditional(profiles = DEFAULT)
public class EhCacheManager implements CacheManager {

	private net.sf.ehcache.CacheManager ehCacheManager;

	@Value("${ehcache.configLocation:classpath:ehcache.xml}")
	private Resource configLocation;

	@Value("${ehcache.lockStripes:0}")
	private int lockStripes = 0;

	private Striped<Lock> stripedLocks;

	@PostConstruct
	public void init() {
		try {
			ehCacheManager = net.sf.ehcache.CacheManager.create(configLocation.getInputStream());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		if (lockStripes <= 0)
			lockStripes = Runtime.getRuntime().availableProcessors() * 4;
		stripedLocks = Striped.lazyWeakLock(lockStripes);
	}

	@PreDestroy
	public void destroy() {
		ehCacheManager.shutdown();
	}

	@Override
	public void put(String key, Object value, int timeToLive, TimeUnit timeUnit, String namespace) {
		put(key, value, -1, timeToLive, timeUnit, namespace);
	}

	@Override
	public void put(String key, Object value, int timeToIdle, int timeToLive, TimeUnit timeUnit, String namespace) {
		if (key == null || value == null)
			return;
		Cache cache = getCache(namespace, true);
		if (cache != null)
			cache.put(new Element(key, value, timeToLive <= 0 ? true : null,
					timeToIdle > 0 ? (int) timeUnit.toSeconds(timeToIdle) : null,
					timeToIdle <= 0 && timeToLive > 0 ? (int) timeUnit.toSeconds(timeToLive) : null));
	}

	@Override
	public boolean exists(String key, String namespace) {
		if (key == null)
			return false;
		Cache cache = getCache(namespace, false);
		if (cache == null)
			return false;
		return cache.isKeyInCache(key);
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
	public Object get(String key, String namespace, int timeToIdle, TimeUnit timeUnit) {
		if (key == null)
			return null;
		Cache cache = getCache(namespace, false);
		if (cache == null)
			return null;
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
	public Map<String, Object> mget(Collection<String> keys, String namespace) {
		if (keys == null)
			return null;
		Cache cache = getCache(namespace, false);
		if (cache == null)
			return null;
		return cache.getAllWithLoader(keys, null);
	}

	@Override
	public void mdelete(Collection<String> keys, String namespace) {
		if (keys == null)
			return;
		Cache cache = getCache(namespace, false);
		if (cache != null)
			for (String key : keys)
				if (StringUtils.isNotBlank(key))
					cache.remove(key);
	}

	@Override
	public boolean containsKey(String key, String namespace) {
		if (key == null)
			return false;
		Cache cache = getCache(namespace, false);
		if (cache != null)
			return cache.isKeyInCache(key);
		else
			return false;
	}

	@Override
	public boolean putIfAbsent(String key, Object value, int timeToLive, TimeUnit timeUnit, String namespace) {
		if (key == null || value == null)
			return false;
		Cache cache = getCache(namespace, true);
		if (cache != null)
			return cache.putIfAbsent(new Element(key, value, timeToLive <= 0 ? true : null, null,
					(int) timeUnit.toSeconds(timeToLive))) == null;
		else
			return false;
	}

	@Override
	public long increment(String key, long delta, int timeToLive, TimeUnit timeUnit, String namespace) {
		if (key == null || delta == 0)
			return -1;
		Cache cache = getCache(namespace, true);
		if (cache != null) {
			Element element = cache.putIfAbsent(new Element(key, new Long(delta), timeToLive <= 0 ? true : null, null,
					(int) timeUnit.toSeconds(timeToLive)));
			if (element == null) {
				return delta;
			} else {
				Lock lock = stripedLocks.get(namespace + ":" + key);
				lock.lock();
				try {
					element = cache.get(key);
					if (element == null) {
						cache.put(new Element(key, new Long(delta), timeToLive <= 0 ? true : null, null,
								(int) timeUnit.toSeconds(timeToLive)));
						return delta;
					} else {
						long value = ((long) element.getObjectValue()) + delta;
						cache.put(new Element(key, new Long(value), timeToLive <= 0 ? true : null, null,
								(int) timeUnit.toSeconds(timeToLive)));
						return value;
					}
				} finally {
					lock.unlock();
				}
			}
		} else
			return -1;
	}

	@Override
	public boolean supportsTimeToIdle() {
		return true;
	}

	@Override
	public boolean supportsUpdateTimeToLive() {
		return true;
	}

	@Override
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
		if (create && cache == null) {
			try {
				ehCacheManager.addCache(namespace);
			} catch (ObjectExistsException e) {
			} catch (Exception e) {
				e.printStackTrace();
			}
			cache = ehCacheManager.getCache(namespace);
		}
		return cache;
	}
}
