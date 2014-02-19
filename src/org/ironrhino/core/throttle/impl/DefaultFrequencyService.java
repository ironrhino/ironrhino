package org.ironrhino.core.throttle.impl;

import java.util.concurrent.TimeUnit;

import org.ironrhino.core.cache.CacheManager;
import org.ironrhino.core.throttle.FrequencyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("frequencyService")
public class DefaultFrequencyService implements FrequencyService {

	private static final String NAMESPACE = "frequency:";

	@Autowired
	private CacheManager cacheManager;

	@Override
	public int available(String name, int limits) {
		Object obj = cacheManager.get(name, NAMESPACE);
		if (obj != null) {
			int current = Integer.valueOf(obj.toString());
			return current > limits ? 0 : limits - current;
		} else
			return limits;
	}

	@Override
	public void increment(String name, long delta, int duration,
			TimeUnit timeUnit) {
		cacheManager.increment(name, delta, duration, timeUnit, NAMESPACE);
	}

}