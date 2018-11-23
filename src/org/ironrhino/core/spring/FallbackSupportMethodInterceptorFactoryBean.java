package org.ironrhino.core.spring;

import java.util.concurrent.atomic.AtomicBoolean;

import org.aopalliance.intercept.MethodInvocation;
import org.ironrhino.core.spring.configuration.Fallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

public abstract class FallbackSupportMethodInterceptorFactoryBean extends MethodInterceptorFactoryBean {

	protected Logger log = LoggerFactory.getLogger(getClass());

	private Object fallback;

	private AtomicBoolean fallbackSearched = new AtomicBoolean();

	@Override
	public Object invoke(MethodInvocation methodInvocation) throws Throwable {
		try {
			return super.invoke(methodInvocation);
		} catch (Throwable ex) {
			if (shouldFallBackFor(ex)) {
				if (!fallbackSearched.get() && fallbackSearched.compareAndSet(false, true)) {
					ApplicationContext ctx = getApplicationContext();
					if (ctx != null) {
						for (String beanName : ctx.getBeanNamesForAnnotation(Fallback.class)) {
							try {
								Class<?> objectType = getObjectType();
								if (objectType == null)
									throw new RuntimeException("Unexpected null");
								this.fallback = ctx.getBean(beanName, objectType);
								log.info("Pick bean {} as fallback of {}", beanName, objectType);
								break;
							} catch (BeansException e) {
								continue;
							}
						}
					}
				}
				if (fallback != null) {
					log.error("Fallback to " + fallback, ex);
					return methodInvocation.getMethod().invoke(fallback, methodInvocation.getArguments());
				}
			}
			throw ex;
		}
	}

	protected abstract boolean shouldFallBackFor(Throwable ex);

}
