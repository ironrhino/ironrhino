package org.ironrhino.core.cache;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public interface CacheManager {

	public void put(String key, Object value, int timeToLive, TimeUnit timeUnit, String namespace);

	public void putWithTti(String key, Object value, int timeToIdle, TimeUnit timeUnit, String namespace);

	public default boolean containsKey(String key, String namespace) {
		return exists(key, namespace);
	}

	public boolean exists(String key, String namespace);

	public Object get(String key, String namespace);

	public Object getWithTti(String key, String namespace, int timeToIdle, TimeUnit timeUnit);

	public long ttl(String key, String namespace);

	public void setTtl(String key, String namespace, int timeToLive, TimeUnit timeUnit);

	public void delete(String key, String namespace);

	public void mput(Map<String, Object> map, int timeToLive, TimeUnit timeUnit, String namespace);

	public Map<String, Object> mget(Set<String> keys, String namespace);

	public void mdelete(Set<String> keys, String namespace);

	public boolean putIfAbsent(String key, Object value, int timeToLive, TimeUnit timeUnit, String namespace);

	public long increment(String key, long delta, int timeToLive, TimeUnit timeUnit, String namespace);

	public boolean supportsTti();

	public boolean supportsGetTtl();

	public boolean supportsUpdateTtl();

}
