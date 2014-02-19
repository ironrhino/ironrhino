package org.ironrhino.core.cache.impl;

import static org.ironrhino.core.metadata.Profiles.DEFAULT;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.ObjectExistsException;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.cache.CacheManager;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component("cacheManager")
@Profile(DEFAULT)
public class EhCacheManager implements CacheManager {

	private net.sf.ehcache.CacheManager ehCacheManager;

	@PostConstruct
	public void init() {
		this.ehCacheManager = new net.sf.ehcache.CacheManager();
	}

	@PreDestroy
	public void destroy() {
		ehCacheManager.shutdown();
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
		Cache cache = getCache(namespace, true);
		if (cache != null)
			cache.put(new Element(key, value, timeToLive <= 0 ? true : null,
					timeToIdle > 0 ? (int) timeUnit.toSeconds(timeToIdle)
							: null,
					timeToIdle <= 0 && timeToLive > 0 ? (int) timeUnit
							.toSeconds(timeToLive) : null));
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
	public Object get(String key, String namespace, int timeToLive,
			TimeUnit timeUnit) {
		if (key == null)
			return null;
		Cache cache = getCache(namespace, false);
		if (cache == null)
			return null;
		Element element = cache.get(key);
		if (element != null) {
			if (element.getTimeToIdle() != timeToLive) {
				element.setTimeToIdle(timeToLive);
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
	public void mput(Map<String, Object> map, int timeToLive,
			TimeUnit timeUnit, String namespace) {
		if (map == null)
			return;
		Cache cache = getCache(namespace, true);
		for (Map.Entry<String, Object> entry : map.entrySet())
			cache.put(new Element(entry.getKey(), entry.getValue(),
					timeToLive <= 0 ? true : null, null,
					timeToLive > 0 ? (int) timeUnit.toSeconds(timeToLive)
							: null));
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
	public boolean putIfAbsent(String key, Object value, int timeToLive,
			TimeUnit timeUnit, String namespace) {
		if (key == null || value == null)
			return false;
		Cache cache = getCache(namespace, true);
		if (cache != null)
			return cache.putIfAbsent(new Element(key, value,
					timeToLive <= 0 ? true : null, null, (int) timeUnit
							.toSeconds(timeToLive))) == null;
		else
			return false;
	}

	@Override
	public long increment(String key, long delta, int timeToLive,
			TimeUnit timeUnit, String namespace) {
		if (key == null || delta == 0)
			return -1;
		Cache cache = getCache(namespace, true);
		if (cache != null) {
			Element element = cache.putIfAbsent(new Element(key,
					new Long(delta), timeToLive <= 0 ? true : null, null,
					(int) timeUnit.toSeconds(timeToLive)));
			if (element == null) {
				return delta;
			} else {
				long value = ((long) element.getObjectValue()) + delta;
				cache.put(new Element(key, new Long(value),
						timeToLive <= 0 ? true : null, null, (int) timeUnit
								.toSeconds(timeToLive)));
				return value;
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
