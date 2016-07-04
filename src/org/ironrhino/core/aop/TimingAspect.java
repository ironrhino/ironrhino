package org.ironrhino.core.aop;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.ironrhino.core.util.ExpressionUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class TimingAspect extends BaseAspect {

	@Autowired
	private Logger logger;

	public TimingAspect() {
		order = Ordered.HIGHEST_PRECEDENCE + 2;
	}

	@Around("execution(public * *(..)) and @annotation(timing)")
	public Object timing(ProceedingJoinPoint jp, Timing timing) throws Throwable {
		long time = System.currentTimeMillis();
		Object result = null;
		Throwable throwable = null;
		try {
			result = jp.proceed();
		} catch (Throwable e) {
			throwable = e;
		}
		time = System.currentTimeMillis() - time;
		String method = jp.getStaticPart().getSignature().toLongString();
		logger.info("method[ {} ] tooks {} ms and {}", method, time, throwable == null ? "success" : "fail");
		if (StringUtils.isNotBlank(timing.value())) {
			Map<String, Object> context = buildContext(jp);
			context.put("method", method);
			context.put("time", time);
			ExpressionUtils.eval(timing.value(), context);
		}
		if (throwable != null)
			throw throwable;
		return result;
	}

}
