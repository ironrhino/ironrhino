package org.ironrhino.core.dataroute;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.ExpressionUtils;
import org.ironrhino.core.util.ReflectionUtils;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class DataRouteInterceptor implements MethodInterceptor {

	@Override
	public Object invoke(MethodInvocation methodInvocation) throws Throwable {
		Method method = methodInvocation.getMethod();
		if (method.isBridge())
			method = BridgeMethodResolver.findBridgedMethod(method);
		Object[] arguments = methodInvocation.getArguments();
		Map<String, Object> paramMap;
		if (arguments.length > 0) {
			String[] names = ReflectionUtils.getParameterNames(method);
			if (names == null)
				throw new RuntimeException("No parameter names discovered for method, please consider using @Param");
			paramMap = new HashMap<>();
			for (int i = 0; i < names.length; i++) {
				paramMap.put(names[i], arguments[i]);
			}
		} else {
			paramMap = Collections.emptyMap();
		}

		Transactional transactional = method.getAnnotation(Transactional.class);
		if (transactional == null)
			transactional = method.getDeclaringClass().getAnnotation(Transactional.class);
		DataRoute dataRoute = method.getAnnotation(DataRoute.class);
		boolean routeOnClass = false;
		if (dataRoute == null) {
			dataRoute = method.getDeclaringClass().getAnnotation(DataRoute.class);
			routeOnClass = dataRoute != null;
		}

		boolean setReadonly = transactional != null
				&& (!DataRouteContext.hasReadonly() || transactional.propagation() == Propagation.REQUIRES_NEW);
		if (setReadonly)
			DataRouteContext.pushReadonly(transactional.readOnly());
		boolean route = false;
		boolean routingKeyPresent = false;
		Object routingKey = null;
		String routerName = null;
		String nodeName = null;
		if (dataRoute != null) {
			route = true;
			nodeName = ExpressionUtils.evalString(dataRoute.nodeName(), paramMap);
			if (!routeOnClass) {
				routingKey = ExpressionUtils.eval(dataRoute.routingKey(), paramMap);
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
			if (setReadonly)
				DataRouteContext.popReadonly();
		}

	}

}
