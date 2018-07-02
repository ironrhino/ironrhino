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

	// timeToLive = 0 not change expiration, timeToLive < 0 permanent
	public long increment(String key, long delta, int timeToLive, TimeUnit timeUnit, String namespace);

	public default long decrement(String key, long delta, int timeToLive, TimeUnit timeUnit, String namespace) {
		if (key == null)
			throw new IllegalArgumentException("key should not be null");
		if (delta <= 0)
			throw new IllegalArgumentException("delta should great than 0");
		return increment(key, -delta, timeToLive, timeUnit, namespace);
	}

	public default long decrementAndReturnNonnegative(String key, long delta, int timeToLive, TimeUnit timeUnit,
			String namespace) {
		if (key == null)
			throw new IllegalArgumentException("key should not be null");
		if (delta <= 0)
			throw new IllegalArgumentException("delta should great than 0");
		long result = increment(key, -delta, timeToLive, timeUnit, namespace);
		if (result < 0) {
			increment(key, delta, timeToLive, timeUnit, namespace);
			throw new IllegalStateException(
					"namespace:" + namespace + ", key:" + key + " does not exist or less than " + delta);
		}
		return result;
	}

	public boolean supportsTti();

	public boolean supportsGetTtl();

	public boolean supportsUpdateTtl();

}
