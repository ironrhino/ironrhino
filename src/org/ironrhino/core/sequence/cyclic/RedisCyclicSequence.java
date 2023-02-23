package org.ironrhino.core.sequence.cyclic;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

import org.ironrhino.core.spring.configuration.PriorityQualifier;
import org.ironrhino.core.util.MaxAttemptsExceededException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.util.Assert;

public class RedisCyclicSequence extends AbstractCyclicSequence {

	public static final String KEY_SEQUENCE = "seq:";

	@Autowired
	@Qualifier("stringRedisTemplate")
	@PriorityQualifier
	private StringRedisTemplate sequenceStringRedisTemplate;

	private BoundValueOperations<String, String> boundValueOperations;

	private RedisScript<Boolean> compareAndSetScript = new DefaultRedisScript<>(
			"if redis.call('get',KEYS[1]) == ARGV[1] then redis.call('set',KEYS[1],ARGV[2]) return true else return false end",
			Boolean.class);

	@Override
	public void afterPropertiesSet() {
		Assert.hasText(getSequenceName(), "sequenceName shouldn't be blank");
		Assert.isTrue(getPaddingLength() > 0, "paddingLength should large than 0");
		int maxlength = String.valueOf(Long.MAX_VALUE).length() - getCycleType().getPattern().length();
		Assert.isTrue(getPaddingLength() <= maxlength, "paddingLength should not large than " + maxlength);
		boundValueOperations = sequenceStringRedisTemplate.boundValueOps(KEY_SEQUENCE + getSequenceName());
		Long time = sequenceStringRedisTemplate.execute((RedisConnection connection) -> connection.time());
		if (time == null)
			throw new RuntimeException("Unexpected null");
		LocalDateTime datetime = LocalDateTime.ofInstant(Instant.ofEpochMilli(time), TimeZone.getDefault().toZoneId());
		boundValueOperations.setIfAbsent(getStringValue(datetime, getPaddingLength(), 0));
	}

	@Override
	public String nextStringValue() {
		int maxAttempts = 3;
		int remainingAttempts = maxAttempts;
		do {
			@SuppressWarnings("unchecked")
			byte[] key = ((RedisSerializer<String>) sequenceStringRedisTemplate.getKeySerializer())
					.serialize(boundValueOperations.getKey());
			List<Object> results = sequenceStringRedisTemplate.executePipelined((RedisConnection connection) -> {
				connection.incr(key);
				connection.time();
				return null;
			});
			long value = (Long) results.get(0);
			LocalDateTime now = LocalDateTime.ofInstant(Instant.ofEpochMilli((Long) results.get(1)),
					TimeZone.getDefault().toZoneId());
			final String stringValue = String.valueOf(value);
			int patternLength = getCycleType().getPattern().length();
			if (stringValue.length() == getPaddingLength() + patternLength) {
				String currentCycle = getCycleType().format(now);
				int comparedValue = currentCycle.compareTo(stringValue.substring(0, patternLength));
				if (comparedValue == 0) {
					// in same cycle
					if (Integer.valueOf(stringValue.substring(patternLength)) != 0) {
						// make sure sequence restart from 0001 not 0000
						// for example 202012120000 increment by 202012119999
						return stringValue;
					}
				} else if (comparedValue < 0) {
					// in same cycle but overflow
					long next = value - Long.valueOf(currentCycle) * ((long) Math.pow(10, getPaddingLength()));
					return currentCycle + next;
				}
			}
			String restart = getStringValue(now, getPaddingLength(), 1);
			Boolean success = sequenceStringRedisTemplate.execute(compareAndSetScript,
					Collections.singletonList(boundValueOperations.getKey()), stringValue, restart);
			if (success == null)
				throw new RuntimeException("Unexpected null");
			if (success)
				return restart;
			try {
				Thread.sleep((1 + maxAttempts - remainingAttempts) * 50);
			} catch (InterruptedException e) {
				logger.warn(e.getMessage(), e);
			}
		} while (--remainingAttempts > 0);
		throw new MaxAttemptsExceededException(maxAttempts);
	}

}