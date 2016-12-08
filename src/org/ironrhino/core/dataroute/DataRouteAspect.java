package org.ironrhino.core.dataroute;

import java.lang.reflect.Method;

import org.aopalliance.aop.Advice;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.transaction.annotation.Transactional;

public class DataRouteAspect extends AbstractPointcutAdvisor {

	private static final long serialVersionUID = -9093221616339043624L;

	private int order = -2;

	private transient final StaticMethodMatcherPointcut pointcut = new StaticMethodMatcherPointcut() {
		public boolean matches(Method method, Class<?> targetClass) {
			if (method.isBridge())
				method = BridgeMethodResolver.findBridgedMethod(method);
			return method.getAnnotation(Transactional.class) != null
					|| targetClass.getAnnotation(Transactional.class) != null
					|| method.getAnnotation(DataRoute.class) != null
					|| targetClass.getAnnotation(DataRoute.class) != null;
		}
	};

	private transient final DataRouteInterceptor interceptor = new DataRouteInterceptor();

	@Override
	public int getOrder() {
		return order;
	}

	@Override
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public Pointcut getPointcut() {
		return pointcut;
	}

	@Override
	public Advice getAdvice() {
		return interceptor;
	}

}
