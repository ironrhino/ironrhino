package org.ironrhino.core.dataroute;

import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.ironrhino.core.aop.BaseAspect;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.service.BaseManager;
import org.ironrhino.core.util.ExpressionUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Aspect
public class DataRouteAspect extends BaseAspect {

	@Qualifier("dataSource")
	private DataSource dataSource;

	public DataRouteAspect() {
		order = -2;
	}

	@Around("execution(public * *(..)) and @annotation(transactional)")
	public Object determineReadonly(ProceedingJoinPoint jp, Transactional transactional) throws Throwable {
		boolean participate = DataRouteContext.hasReadonly() && transactional.propagation() != Propagation.REQUIRES_NEW;
		if (participate) {
			return jp.proceed();
		} else {
			DataRouteContext.pushReadonly(transactional.readOnly());
			try {
				return jp.proceed();
			} finally {
				DataRouteContext.popReadonly();
			}
		}
	}

	@Around("execution(public * *(..)) and @annotation(dataRoute)")
	public Object routeOnMethod(ProceedingJoinPoint jp, DataRoute dataRoute) throws Throwable {
		Map<String, Object> context = buildContext(jp);
		Object routingKey = ExpressionUtils.eval(dataRoute.routingKey(), context);
		String routerName = dataRoute.routerName();
		String nodeName = ExpressionUtils.evalString(dataRoute.nodeName(), context);
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
			return jp.proceed();
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

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Around("execution(public * *(..)) and target(baseManager) and @annotation(transactional)")
	public Object routeOnClass(ProceedingJoinPoint jp, BaseManager baseManager, Transactional transactional)
			throws Throwable {
		DataRoute dataRoute = null;
		Object target = jp.getTarget();
		if (target != null)
			dataRoute = target.getClass().getAnnotation(DataRoute.class);
		if (dataRoute == null) {
			Class<Persistable<?>> entityClass = baseManager.getEntityClass();
			if (entityClass != null)
				dataRoute = entityClass.getAnnotation(DataRoute.class);
		}
		String nodeName = null;
		if (dataRoute != null) {
			Map<String, Object> context = buildContext(jp);
			nodeName = ExpressionUtils.evalString(dataRoute.nodeName(), context);
			if (StringUtils.isNotBlank(nodeName))
				DataRouteContext.setNodeName(nodeName);
		}
		try {
			return jp.proceed();
		} finally {
			if (StringUtils.isNotBlank(nodeName))
				DataRouteContext.removeNodeName(nodeName);
		}
	}

}
