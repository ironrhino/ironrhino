package org.ironrhino.core.spring;

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.ironrhino.core.util.ReflectionUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.FactoryBean;

public abstract class MethodInterceptorFactoryBean implements MethodInterceptor, FactoryBean<Object> {

	@Override
	public Object invoke(final MethodInvocation methodInvocation) throws Throwable {
		Method method = methodInvocation.getMethod();
		if (AopUtils.isToStringMethod(methodInvocation.getMethod()))
			return "Dynamic proxy for  [" + getObjectType().getName() + "]";
		if (method.isDefault()) {
			return ReflectionUtils.invokeDefaultMethod(getObject(), method, methodInvocation.getArguments());
		}
		return doInvoke(methodInvocation);
	}

	protected abstract Object doInvoke(MethodInvocation methodInvocation) throws Throwable;

}
