package org.ironrhino.core.sequence;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.util.Assert;

public class RedisCyclicSequence extends AbstractCyclicSequence {

	public static final String KEY_SEQUENCE = "{seq}:";

	@Autowired
	@Qualifier("stringRedisTemplate")
	private RedisTemplate<String, String> stringRedisTemplate;

	private BoundValueOperations<String, String> boundValueOperations;

	public void setStringRedisTemplate(
			RedisTemplate<String, String> stringRedisTemplate) {
		this.stringRedisTemplate = stringRedisTemplate;
	}

	@Override
	public void afterPropertiesSet() {
		Assert.hasText(getSequenceName());
		Assert.isTrue(getPaddingLength() > 0);
		boundValueOperations = stringRedisTemplate.boundValueOps(KEY_SEQUENCE
				+ getSequenceName());
		boundValueOperations.setIfAbsent(getStringValue(new Date(),
				getPaddingLength(), 0));
	}

	@Override
	public String nextStringValue() {
		long value = boundValueOperations.increment(1);
		final String stringValue = String.valueOf(value);
		Calendar cal = Calendar.getInstance();
		CycleType cycleType = getCycleType();
		if (cycleType.ordinal() <= CycleType.MINUTE.ordinal())
			cal.set(Calendar.MINUTE,
					Integer.valueOf(stringValue.substring(10, 12)));
		if (cycleType.ordinal() <= CycleType.HOUR.ordinal())
			cal.set(Calendar.HOUR_OF_DAY,
					Integer.valueOf(stringValue.substring(8, 10)));
		if (cycleType.ordinal() <= CycleType.DAY.ordinal())
			cal.set(Calendar.DAY_OF_MONTH,
					Integer.valueOf(stringValue.substring(6, 8)));
		if (cycleType.ordinal() <= CycleType.MONTH.ordinal())
			cal.set(Calendar.MONTH,
					Integer.valueOf(stringValue.substring(4, 6)) - 1);
		if (cycleType.ordinal() <= CycleType.YEAR.ordinal())
			cal.set(Calendar.YEAR, Integer.valueOf(stringValue.substring(0, 4)));
		Date d = cal.getTime();
		if (getCycleType().isSameCycle(d, new Date()))
			return stringValue;

		final String restart = getStringValue(new Date(), getPaddingLength(), 1);
		boolean success = stringRedisTemplate
				.execute(new SessionCallback<Boolean>() {
					@Override
					@SuppressWarnings({ "unchecked", "rawtypes" })
					public Boolean execute(RedisOperations operations) {
						operations.watch(Collections
								.singleton(boundValueOperations.getKey()));
						if (stringValue.equals(boundValueOperations.get())) {
							operations.multi();
							boundValueOperations.set(restart);
							return operations.exec() != null;
						} else {
							return false;
						}
					}
				});
		return success ? restart : nextStringValue();
	}

}