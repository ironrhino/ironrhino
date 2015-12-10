package org.ironrhino.core.throttle;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.ironrhino.core.aop.BaseAspect;
import org.ironrhino.core.coordination.LockService;
import org.ironrhino.core.spring.NameGenerator;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.ErrorMessage;
import org.ironrhino.core.util.ExpressionUtils;
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
			Class<?> beanClass = jp.getTarget().getClass();
			String beanName = NameGenerator.buildDefaultBeanName(beanClass.getName());
			Component comp = beanClass.getAnnotation(Component.class);
			if (StringUtils.isNotBlank(comp.value()))
				beanName = comp.value();
			sb.append(beanName).append('.').append(jp.getSignature().getName()).append('(');
			Object[] args = jp.getArgs();
			if (args.length > 0) {
				for (int i = 0; i < args.length; i++) {
					sb.append(args[i]);
					if (i != args.length - 1)
						sb.append(',');
				}
			}
			sb.append(')');
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
			throw new ErrorMessage("tryLock failed and skip execute [" + jp.getSignature() + "]");
		}
	}

}
