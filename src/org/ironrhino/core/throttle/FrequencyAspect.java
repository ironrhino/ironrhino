package org.ironrhino.core.throttle;

import java.util.Map;
import java.util.concurrent.TimeUnit;

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
		int duration = (int) frequency.timeUnit().toMillis(frequency.duration());
		long timestamp = System.currentTimeMillis();
		long windowStart = (timestamp - timestamp % duration);
		int elapsed = (int) (timestamp - windowStart);
		int remaining = duration - elapsed;
		String currentWindowKey = key + ":" + windowStart;
		long lastWindowUsed = cacheManager.increment(key + ":" + (windowStart - duration), 0, remaining,
				TimeUnit.MILLISECONDS, NAMESPACE);
		long currentWindowUsed = cacheManager.increment(currentWindowKey, 1, duration + remaining,
				TimeUnit.MILLISECONDS, NAMESPACE);
		long limits = ExpressionUtils.evalLong(frequency.limits(), context, 0);
		if (lastWindowUsed == 0 && limits >= currentWindowUsed || lastWindowUsed > 0
				&& limits >= (long) (currentWindowUsed + lastWindowUsed * (1 - (double) elapsed / duration))) {
			return jp.proceed();
		} else {
			cacheManager.increment(currentWindowKey, -1, duration + remaining, TimeUnit.MILLISECONDS, NAMESPACE);
			// revert increment
			throw new FrequencyLimitExceededException(key);
		}
	}

}
