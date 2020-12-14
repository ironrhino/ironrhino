package org.ironrhino.core.throttle;

import java.util.Map;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.ironrhino.core.aop.BaseAspect;
import org.ironrhino.core.cache.CacheManager;
import org.ironrhino.core.util.ExpressionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class FrequencyAspect extends BaseAspect {

	private static final String NAMESPACE = "frequency";

	@Autowired
	private CacheManager cacheManager;

	public FrequencyAspect() {
		order = -1000;
	}

	@Around("execution(public * *(..)) and @annotation(frequency)")
	public Object control(ProceedingJoinPoint jp, Frequency frequency) throws Throwable {
		Map<String, Object> context = buildContext(jp);
		String key = frequency.key();
		if (!key.isEmpty()) {
			key = ExpressionUtils.evalString(key, context);
		} else {
			key = buildKey(jp);
		}
		long timestamp = System.currentTimeMillis();
		long duration = frequency.timeUnit().toMillis(frequency.duration());
		long windowStart = (timestamp - timestamp % duration);
		String actualKey = key + ":" + windowStart; // Fixed Window
		int limits = ExpressionUtils.evalInt(frequency.limits(), context, 0);
		int used = (int) cacheManager.increment(actualKey, 1, frequency.duration(), frequency.timeUnit(), NAMESPACE);
		if (limits >= used) {
			return jp.proceed();
		} else {
			throw new FrequencyLimitExceededException(key);
		}
	}

}
