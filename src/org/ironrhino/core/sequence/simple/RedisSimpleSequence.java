package org.ironrhino.core.sequence.simple;

import org.ironrhino.core.spring.configuration.PriorityQualifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.Assert;

public class RedisSimpleSequence extends AbstractSimpleSequence {

	public static final String KEY_SEQUENCE = "seq:";

	@Autowired
	@Qualifier("stringRedisTemplate")
	@PriorityQualifier
	private StringRedisTemplate sequenceStringRedisTemplate;

	private BoundValueOperations<String, String> boundValueOperations;

	@Override
	public void afterPropertiesSet() {
		Assert.hasText(getSequenceName(), "sequenceName shouldn't be blank");
		Assert.isTrue(getPaddingLength() > 0, "paddingLength should large than 0");
		boundValueOperations = sequenceStringRedisTemplate.boundValueOps(KEY_SEQUENCE + getSequenceName());
		boundValueOperations.setIfAbsent("0");
	}

	@Override
	public void restart() {
		boundValueOperations.set("0");
	}

	@Override
	public int nextIntValue() {
		Long value = boundValueOperations.increment(1);
		if (value == null)
			throw new RuntimeException("Unexpected null");
		return value.intValue();
	}

}