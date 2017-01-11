package org.ironrhino.core.aop;

import java.util.HashMap;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.ironrhino.core.servlet.RequestContext;
import org.ironrhino.core.util.AuthzUtils;
import org.ironrhino.core.util.ReflectionUtils;
import org.springframework.aop.support.AbstractPointcutAdvisor;

public abstract class AbstractMethodInterceptor<ASPECT extends AbstractPointcutAdvisor> implements MethodInterceptor {

	private Class<?> aspectClass;

	public AbstractMethodInterceptor() {
		aspectClass = (Class<?>) ReflectionUtils.getGenericClass(getClass());
	}

	protected boolean isBypass() {
		return AopContext.isBypass(aspectClass);
	}

	protected Map<String, Object> buildContext(MethodInvocation methodInvocation) {
		Map<String, Object> context = new HashMap<>();
		Object[] args = methodInvocation.getArguments();
		String[] paramNames = ReflectionUtils.getParameterNames(methodInvocation.getMethod());
		if (paramNames == null) {
			throw new RuntimeException("No parameter names discovered for method, please consider using @Param");
		} else {
			for (int i = 0; i < args.length; i++)
				context.put(paramNames[i], args[i]);
		}
		context.put(AopContext.CONTEXT_KEY_THIS, methodInvocation.getThis());
		context.put(AopContext.CONTEXT_KEY_METHOD_NAME, methodInvocation.getMethod().getName());
		context.put(AopContext.CONTEXT_KEY_ARGS, methodInvocation.getArguments());
		context.put(AopContext.CONTEXT_KEY_REQUEST, RequestContext.getRequest());
		context.put(AopContext.CONTEXT_KEY_USER, AuthzUtils.getUserDetails());
		return context;
	}

	protected void putReturnValueIntoContext(Map<String, Object> context, Object value) {
		String oldName = AopContext.CONTEXT_KEY_RETVAL.substring(1);
		if (!context.containsKey(oldName))
			context.put(oldName, value);
		context.put(AopContext.CONTEXT_KEY_RETVAL, value);
	}

}
