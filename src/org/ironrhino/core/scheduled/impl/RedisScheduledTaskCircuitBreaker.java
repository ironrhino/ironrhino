package org.ironrhino.core.scheduled.impl;

import static org.ironrhino.core.metadata.Profiles.CLOUD;
import static org.ironrhino.core.metadata.Profiles.CLUSTER;
import static org.ironrhino.core.metadata.Profiles.DUAL;

import org.ironrhino.core.scheduled.ScheduledTaskCircuitBreaker;
import org.ironrhino.core.spring.configuration.ServiceImplementationConditional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component("scheduledTaskCircuitBreaker")
@ServiceImplementationConditional(profiles = { DUAL, CLUSTER, CLOUD })
public class RedisScheduledTaskCircuitBreaker implements ScheduledTaskCircuitBreaker {

	private static final String NAMESPACE_SHORT_CIRCUIT = "ShortCircuit:";

	@Autowired
	@Qualifier("stringRedisTemplate")
	private RedisTemplate<String, String> stringRedisTemplate;

	@Override
	public boolean isShortCircuit(String task) {
		String s = stringRedisTemplate.opsForValue().get(NAMESPACE_SHORT_CIRCUIT + task);
		return s != null && s.equals("true");
	}

	@Override
	public void setShortCircuit(String task, boolean value) {
		if (!value)
			stringRedisTemplate.delete(NAMESPACE_SHORT_CIRCUIT + task);
		else
			stringRedisTemplate.opsForValue().set(NAMESPACE_SHORT_CIRCUIT + task, String.valueOf(value));
	}
}
