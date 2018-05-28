package org.ironrhino.core.dataroute;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.ironrhino.core.aop.BaseAspect;
import org.ironrhino.core.util.ExpressionUtils;

@Aspect
public class DataRouteAspect extends BaseAspect {

	public DataRouteAspect() {
		order = -2;
	}

	@Around("execution(public * *(..)) and @annotation(dataRoute)")
	public Object routeByMethod(ProceedingJoinPoint pjp, DataRoute dataRoute) throws Throwable {
		Map<String, Object> context = buildContext(pjp);
		String nodeName = ExpressionUtils.evalString(dataRoute.nodeName(), context);
		Object routingKey = ExpressionUtils.eval(dataRoute.routingKey(), context);
		String routerName = dataRoute.routerName();
		boolean routingKeyPresent = !(routingKey == null
				|| routingKey instanceof String && StringUtils.isBlank((String) routingKey));
		if (routingKeyPresent) {
			DataRouteContext.setRoutingKey(routingKey);
			if (StringUtils.isNotBlank(routerName))
				DataRouteContext.setRouterName(routerName);
		} else {
			if (StringUtils.isNotBlank(nodeName))
				DataRouteContext.setNodeName(nodeName);
		}
		try {
			return pjp.proceed();
		} finally {
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

	@Around("execution(public * *(..)) and @within(dataRoute) and not @annotation(org.ironrhino.core.dataroute.DataRoute)")
	public Object routeByClass(ProceedingJoinPoint pjp, DataRoute dataRoute) throws Throwable {
		String nodeName = dataRoute.nodeName();
		DataRouteContext.setNodeName(nodeName);
		try {
			return pjp.proceed();
		} finally {
			DataRouteContext.removeNodeName(nodeName);
		}
	}

}
