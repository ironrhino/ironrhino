package org.ironrhino.core.throttle;

import java.util.concurrent.TimeUnit;

import org.ironrhino.core.util.IllegalConcurrentAccessException;

public interface ThrottleService {

	long DEFAULT_MAX_SLEEP_TIME = 30000;

	public void delay(String key, int interval, TimeUnit timeUnit, int initialDelay)
			throws IllegalConcurrentAccessException;

}