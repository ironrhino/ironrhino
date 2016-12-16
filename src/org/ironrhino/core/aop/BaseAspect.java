package org.ironrhino.core.aop;

import java.util.HashMap;
import java.util.Map;

import org.aspectj.lang.JoinPoint;
import org.ironrhino.core.servlet.RequestContext;
import org.ironrhino.core.util.AuthzUtils;
import org.ironrhino.core.util.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;

public class BaseAspect implements Ordered {

	protected Logger logger = LoggerFactory.getLogger(getClass());

	protected int order;

	protected boolean isBypass() {
		return AopContext.isBypass(this.getClass());
	}

	@Override
	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	protected Map<String, Object> buildContext(JoinPoint jp) {
		Map<String, Object> context = new HashMap<>();
		Object[] args = jp.getArgs();
		String[] paramNames = ReflectionUtils.getParameterNames(jp);
		if (paramNames == null) {
			throw new RuntimeException("No parameter names discovered for method, please consider using @Param");
		} else {
			for (int i = 0; i < args.length; i++)
				context.put(paramNames[i], args[i]);
		}
		context.put(AopContext.CONTEXT_KEY_THIS, jp.getThis());
		context.put(AopContext.CONTEXT_KEY_ARGS, jp.getArgs());
		context.put(AopContext.CONTEXT_KEY_REQUEST, RequestContext.getRequest());
		context.put(AopContext.CONTEXT_KEY_USER, AuthzUtils.getUserDetails());
		return context;
	}

	protected void putReturnValueIntoContext(Map<String, Object> context, Object value) {
		context.put(AopContext.CONTEXT_KEY_RETVAL, value);
	}

}
