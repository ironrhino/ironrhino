package org.ironrhino.core.throttle;

import java.util.concurrent.TimeUnit;

public interface FrequencyService {

	public int available(String name, int limits);

	public void increment(String name, long delta, int duration,
			TimeUnit timeUnit);

}