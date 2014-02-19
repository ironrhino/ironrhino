package org.ironrhino.core.aop;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.ironrhino.core.util.ExpressionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class TimingAspect extends BaseAspect {

	private Logger logger = LoggerFactory.getLogger(getClass());

	public TimingAspect() {
		order = -1000;
	}

	@Around("execution(public * *(..)) and @annotation(timing)")
	public Object timing(ProceedingJoinPoint jp, Timing timing)
			throws Throwable {
		long time = System.currentTimeMillis();
		Object result = jp.proceed();
		time = System.currentTimeMillis() - time;
		String method = jp.getStaticPart().getSignature().toLongString();
		logger.info("{} takes {} ms", method, time);
		if (StringUtils.isNotBlank(timing.value())) {
			Map<String, Object> context = buildContext(jp);
			context.put("method", method);
			context.put("time", time);
			ExpressionUtils.eval(timing.value(), context);
		}
		return result;
	}

}
