package org.ironrhino.core.throttle;

import java.util.concurrent.TimeUnit;

import org.ironrhino.core.util.IllegalConcurrentAccessException;

public interface ThrottleService {

	public void delay(String key, int interval, TimeUnit timeUnit, int initialDelay)
			throws IllegalConcurrentAccessException;

}