package org.ironrhino.core.dataroute;

import java.lang.reflect.Method;

import org.aopalliance.aop.Advice;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.annotation.AnnotationUtils;

import lombok.Getter;
import lombok.Setter;

public class DataRouteAspect extends AbstractPointcutAdvisor {

	private static final long serialVersionUID = -9093221616339043624L;

	@Getter
	@Setter
	private int order = -2;

	private transient final StaticMethodMatcherPointcut pointcut = new StaticMethodMatcherPointcut() {
		@Override
		public boolean matches(Method method, Class<?> targetClass) {
			method = BridgeMethodResolver.findBridgedMethod(method);
			return AnnotationUtils.findAnnotation(method, DataRoute.class) != null
					|| AnnotationUtils.findAnnotation(targetClass, DataRoute.class) != null;
		}
	};

	private transient final DataRouteInterceptor interceptor = new DataRouteInterceptor();

	@Override
	public Pointcut getPointcut() {
		return pointcut;
	}

	@Override
	public Advice getAdvice() {
		return interceptor;
	}

}
