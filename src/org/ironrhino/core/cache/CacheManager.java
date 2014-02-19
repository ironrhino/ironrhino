package org.ironrhino.core.cache;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public interface CacheManager {

	String DEFAULT_TIME_TO_LIVE = "3600";

	String DEFAULT_TIME_TO_IDLE = "-1";

	public void put(String key, Object value, int timeToLive,
			TimeUnit timeUnit, String namespace);

	public void put(String key, Object value, int timeToIdle, int timeToLive,
			TimeUnit timeUnit, String namespace);

	public Object get(String key, String namespace);

	public Object get(String key, String namespace, int timeToLive,
			TimeUnit timeUnit);

	public void delete(String key, String namespace);

	public void mput(Map<String, Object> map, int timeToLive,
			TimeUnit timeUnit, String namespace);

	public Map<String, Object> mget(Collection<String> keys, String namespace);

	public void mdelete(Collection<String> keys, String namespace);

	public boolean containsKey(String key, String namespace);

	public boolean putIfAbsent(String key, Object value, int timeToLive,
			TimeUnit timeUnit, String namespace);

	public long increment(String key, long delta, int timeToLive,
			TimeUnit timeUnit, String namespace);

	public boolean supportsTimeToIdle();

	public boolean supportsUpdateTimeToLive();

}
