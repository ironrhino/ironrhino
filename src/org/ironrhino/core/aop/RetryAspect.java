package org.ironrhino.core.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class RetryAspect extends BaseAspect {

	public RetryAspect() {
		order = -200;
	}

	private boolean isNeedToRetry(Throwable t, Class<?>[] recoverableExceptions) {
		for (Class<?> exp : recoverableExceptions)
			if (exp.isInstance(t))
				return true;
		return false;
	}

	private long getRetryInterval(int times, long interval,
			long incrementalFactor) {
		return interval + (times * incrementalFactor);
	}

	@Around("@annotation(retryConfig)")
	public Object process(ProceedingJoinPoint pjp, RetryConfig retryConfig)
			throws Throwable {
		int count = 0;
		Throwable fault;
		Class<?>[] recoverableExceptions = retryConfig.recoverableExceptions();
		int times = retryConfig.maxTimes();
		long incrementalFactor = retryConfig.incrementalFactor();
		do {
			try {
				return pjp.proceed();
			} catch (Throwable t) {
				fault = t;
				if (!isNeedToRetry(t, recoverableExceptions)) {
					break;
				}
				Thread.sleep(getRetryInterval(times, retryConfig.interval(),
						incrementalFactor));
			}
			count++;
		} while (count < (times + 1));
		throw fault;

	}

}
