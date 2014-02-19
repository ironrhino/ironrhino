package org.ironrhino.core.aop;

import java.util.HashMap;
import java.util.Map;

import org.aspectj.lang.JoinPoint;
import org.ironrhino.core.util.AuthzUtils;
import org.ironrhino.core.util.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;

public class BaseAspect implements Ordered {

	protected Logger log = LoggerFactory.getLogger(getClass());

	protected int order;

	private static boolean warnNoDebugSymbolInformation;

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
		Map<String, Object> context = new HashMap<String, Object>();
		Object[] args = jp.getArgs();
		String[] paramNames = ReflectionUtils.getParameterNames(jp);
		if (paramNames == null) {
			if (!warnNoDebugSymbolInformation) {
				warnNoDebugSymbolInformation = true;
				log.warn("Unable to resolve method parameter names for method: "
						+ jp.getStaticPart().getSignature()
						+ ". Debug symbol information is required if you are using parameter names in expressions.");
			}
		} else {
			for (int i = 0; i < args.length; i++)
				context.put(paramNames[i], args[i]);
		}
		if (!context.containsKey("_this"))
			context.put("_this", jp.getThis());
		if (!context.containsKey("target"))
			context.put("target", jp.getTarget());
		if (!context.containsKey("aspect"))
			context.put("aspect", this);
		if (!context.containsKey("args"))
			context.put("args", jp.getArgs());
		if (!context.containsKey("user"))
			context.put("user", AuthzUtils.getUserDetails());
		return context;
	}

	protected void putReturnValueIntoContext(Map<String, Object> context,
			Object value) {
		context.put("retval", value);
	}

}
