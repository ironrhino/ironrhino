package org.ironrhino.core.scheduled.impl;

import static org.ironrhino.core.metadata.Profiles.CLOUD;
import static org.ironrhino.core.metadata.Profiles.CLUSTER;
import static org.ironrhino.core.metadata.Profiles.DUAL;

import org.ironrhino.core.scheduled.ScheduledTaskCircuitBreaker;
import org.ironrhino.core.spring.configuration.PrioritizedQualifier;
import org.ironrhino.core.spring.configuration.ServiceImplementationConditional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component("scheduledTaskCircuitBreaker")
@ServiceImplementationConditional(profiles = { DUAL, CLUSTER, CLOUD })
public class RedisScheduledTaskCircuitBreaker implements ScheduledTaskCircuitBreaker {

	private static final String NAMESPACE_SHORT_CIRCUIT = "ShortCircuit:";

	@Autowired
	@PrioritizedQualifier("cacheRedisTemplate")
	private RedisTemplate<String, Object> redisTemplate;

	@Override
	public boolean isShortCircuit(String task) {
		Boolean b = (Boolean) redisTemplate.opsForValue().get(NAMESPACE_SHORT_CIRCUIT + task);
		return b != null && b;
	}

	@Override
	public void setShortCircuit(String task, boolean value) {
		redisTemplate.opsForValue().set(NAMESPACE_SHORT_CIRCUIT + task, value);
	}
}
