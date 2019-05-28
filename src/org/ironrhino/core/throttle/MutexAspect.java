package org.ironrhino.core.throttle;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.ironrhino.core.aop.BaseAspect;
import org.ironrhino.core.coordination.LockService;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.ExpressionUtils;
import org.ironrhino.core.util.LockFailedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class MutexAspect extends BaseAspect {

	@Autowired
	private LockService lockService;

	public MutexAspect() {
		order = -2000;
	}

	@Around("execution(public * *(..)) and @annotation(mutex)")
	public Object control(ProceedingJoinPoint jp, Mutex mutex) throws Throwable {
		String key = mutex.key();
		StringBuilder sb = new StringBuilder();
		if (StringUtils.isBlank(key)) {
			sb.append(buildKey(jp));
		} else {
			Map<String, Object> context = buildContext(jp);
			sb.append(ExpressionUtils.evalString(key, context));
		}
		switch (mutex.scope()) {
		case GLOBAL:
			break;
		case APPLICATION:
			sb.append('-').append(AppInfo.getAppName());
			break;
		case LOCAL:
			sb.append('-').append(AppInfo.getAppName()).append('-').append(AppInfo.getHostName());
			break;
		default:
			break;
		}
		String lockName = sb.toString();
		if (lockService.tryLock(lockName)) {
			try {
				return jp.proceed();
			} finally {
				lockService.unlock(lockName);
			}
		} else {
			throw new LockFailedException(buildKey(jp));
		}
	}

}
