package org.ironrhino.core.cache;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public interface CacheManager {

	void put(String key, Object value, int timeToLive, TimeUnit timeUnit, String namespace);

	void putWithTti(String key, Object value, int timeToIdle, TimeUnit timeUnit, String namespace);

	default boolean containsKey(String key, String namespace) {
		return exists(key, namespace);
	}

	boolean exists(String key, String namespace);

	Object get(String key, String namespace);

	Object getWithTti(String key, String namespace, int timeToIdle, TimeUnit timeUnit);

	long ttl(String key, String namespace);

	void setTtl(String key, String namespace, int timeToLive, TimeUnit timeUnit);

	void delete(String key, String namespace);

	void mput(Map<String, Object> map, int timeToLive, TimeUnit timeUnit, String namespace);

	Map<String, Object> mget(Collection<String> keys, String namespace);

	void mdelete(Collection<String> keys, String namespace);

	boolean putIfAbsent(String key, Object value, int timeToLive, TimeUnit timeUnit, String namespace);

	// timeToLive = 0 not change expiration, timeToLive < 0 permanent
	long increment(String key, long delta, int timeToLive, TimeUnit timeUnit, String namespace);

	default long decrement(String key, long delta, int timeToLive, TimeUnit timeUnit, String namespace) {
		return increment(key, -delta, timeToLive, timeUnit, namespace);
	}

	default long decrementAndReturnNonnegative(String key, long delta, int timeToLive, TimeUnit timeUnit,
			String namespace) {
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

	boolean supportsTti();

	boolean supportsGetTtl();

	boolean supportsUpdateTtl();

}
