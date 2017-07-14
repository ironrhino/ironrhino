package org.ironrhino.core.throttle.impl;

import java.util.concurrent.TimeUnit;

import org.ironrhino.core.cache.CacheManager;
import org.ironrhino.core.throttle.ThrottleService;
import org.ironrhino.core.util.IllegalConcurrentAccessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("throttleService")
public class DefaultThrottleService implements ThrottleService {

	private static final String NAMESPACE = "throttle";

	private static final String KEY_SUFFIX_DELAY = "$$delay";

	private static final String KEY_SUFFIX_CONCURRENT = "$$concurrent";

	@Autowired
	private CacheManager cacheManager;

	@Value("${throttleService.maxSleepTime:" + DEFAULT_MAX_SLEEP_TIME + "}")
	private long maxSleepTime = DEFAULT_MAX_SLEEP_TIME;

	@Override
	public void delay(String key, int interval, TimeUnit timeUnit, int initialDelay)
			throws IllegalConcurrentAccessException {
		if (key == null)
			return;
		String dkey = key + KEY_SUFFIX_DELAY;
		String ckey = key + KEY_SUFFIX_CONCURRENT;
		long ttl = -1;
		try {
			ttl = cacheManager.ttl(dkey, NAMESPACE);
		} catch (UnsupportedOperationException e) {
			if (cacheManager.exists(dkey, NAMESPACE))
				ttl = timeUnit.toMillis(interval);
		}
		if (ttl <= 0) {
			if (!cacheManager.putIfAbsent(ckey, "",
					(int) Math.max(TimeUnit.MILLISECONDS.convert(initialDelay, timeUnit), 500), TimeUnit.MILLISECONDS,
					NAMESPACE))
				throw new IllegalConcurrentAccessException(key);
			if (initialDelay > 0)
				try {
					Thread.sleep(Math.min(timeUnit.toMillis(initialDelay), maxSleepTime));
				} catch (InterruptedException e) {
				}
		} else {
			if (!cacheManager.putIfAbsent(ckey, "", (int) ttl, TimeUnit.MILLISECONDS, NAMESPACE))
				throw new IllegalConcurrentAccessException(key);
			try {
				Thread.sleep(Math.min(ttl, maxSleepTime));
			} catch (InterruptedException e) {
			}
		}
		cacheManager.put(dkey, "", interval, timeUnit, NAMESPACE);
	}

}