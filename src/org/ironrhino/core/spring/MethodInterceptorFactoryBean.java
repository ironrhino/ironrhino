package org.ironrhino.core.spring;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.util.ClassUtils;

public abstract class MethodInterceptorFactoryBean implements MethodInterceptor, FactoryBean<Object> {

	private final Map<Method, MethodHandle> defaultMethods = new ConcurrentHashMap<>();

	@Override
	public Object invoke(final MethodInvocation methodInvocation) throws Throwable {
		Method method = methodInvocation.getMethod();
		if (AopUtils.isToStringMethod(methodInvocation.getMethod()))
			return "Dynamic proxy for  [" + getObjectType().getName() + "]";
		if (method.isDefault()) {
			Class<?> objectType = getObjectType();
			Object object = getObject();
			MethodHandle mh = defaultMethods.computeIfAbsent(method, m -> {
				try {
					if (ClassUtils.isPresent("java.lang.StackWalker", System.class.getClassLoader())) {
						// jdk 9 and later
						return MethodHandles.lookup()
								.findSpecial(objectType, m.getName(),
										MethodType.methodType(m.getReturnType(), m.getParameterTypes()), objectType)
								.bindTo(object);
					} else {
						Constructor<Lookup> constructor = Lookup.class.getDeclaredConstructor(Class.class);
						constructor.setAccessible(true);
						return constructor.newInstance(objectType).in(objectType).unreflectSpecial(method, objectType)
								.bindTo(object);
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
			try {
				return mh.invokeWithArguments(methodInvocation.getArguments());
			} catch (Exception e) {
				throw e;
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}
		return doInvoke(methodInvocation);
	}

	protected abstract Object doInvoke(MethodInvocation methodInvocation) throws Throwable;

}
