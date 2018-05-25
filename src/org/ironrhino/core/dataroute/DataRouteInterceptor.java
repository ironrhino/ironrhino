package org.ironrhino.core.dataroute;

import java.lang.reflect.Method;
import java.util.Map;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.aop.AbstractMethodInterceptor;
import org.ironrhino.core.util.ExpressionUtils;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.annotation.AnnotationUtils;

public class DataRouteInterceptor extends AbstractMethodInterceptor<DataRouteAspect> {

	@Override
	public Object invoke(MethodInvocation methodInvocation) throws Throwable {
		Method method = methodInvocation.getMethod();
		method = BridgeMethodResolver.findBridgedMethod(method);
		DataRoute dataRoute = AnnotationUtils.findAnnotation(method, DataRoute.class);
		boolean routeOnClass = false;
		if (dataRoute == null) {
			dataRoute = AnnotationUtils.findAnnotation(method.getDeclaringClass(), DataRoute.class);
			routeOnClass = dataRoute != null;
		}
		boolean route = false;
		boolean routingKeyPresent = false;
		Object routingKey = null;
		String routerName = null;
		String nodeName = null;
		if (dataRoute != null) {
			Map<String, Object> context = buildContext(methodInvocation);
			route = true;
			nodeName = ExpressionUtils.evalString(dataRoute.nodeName(), context);
			if (!routeOnClass) {
				routingKey = ExpressionUtils.eval(dataRoute.routingKey(), context);
				routerName = dataRoute.routerName();
			}
			routingKeyPresent = !routeOnClass && !(routingKey == null
					|| routingKey instanceof String && StringUtils.isBlank((String) routingKey));
			if (routingKeyPresent) {
				DataRouteContext.setRoutingKey(routingKey);
				if (StringUtils.isNotBlank(routerName))
					DataRouteContext.setRouterName(routerName);
			} else {
				if (StringUtils.isNotBlank(nodeName))
					DataRouteContext.setNodeName(nodeName);
			}
		}
		try {
			return methodInvocation.proceed();
		} finally {
			if (route) {
				if (routingKeyPresent) {
					DataRouteContext.removeRoutingKey(routingKey);
					if (StringUtils.isNotBlank(routerName))
						DataRouteContext.removeRouterName(routerName);
				} else {
					if (StringUtils.isNotBlank(nodeName))
						DataRouteContext.removeNodeName(nodeName);
				}
			}
		}

	}

}
