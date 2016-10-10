package org.ironrhino.core.scheduled.impl;

import static org.ironrhino.core.metadata.Profiles.DEFAULT;

import java.util.HashMap;
import java.util.Map;

import org.ironrhino.core.scheduled.ScheduledTaskCircuitBreaker;
import org.ironrhino.core.spring.configuration.ServiceImplementationConditional;
import org.springframework.stereotype.Component;

@Component("scheduledTaskCircuitBreaker")
@ServiceImplementationConditional(profiles = { DEFAULT })
public class StandaloneScheduledTaskCircuitBreaker implements ScheduledTaskCircuitBreaker {

	private Map<String, Boolean> map = new HashMap<>();

	@Override
	public boolean isShortCircuit(String task) {
		Boolean b = map.get(task);
		return b != null && b;
	}

	@Override
	public void setShortCircuit(String task, boolean value) {
		map.put(task, value);
	}

}
