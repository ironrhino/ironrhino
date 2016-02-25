package org.ironrhino.core.dataroute;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.ironrhino.core.aop.BaseAspect;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.service.BaseManager;
import org.ironrhino.core.util.ExpressionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * split read and write database
 * 
 * @author zhouyanming
 * @see org.ironrhino.core.dataroute.RoutingDataSource
 * @see org.ironrhino.core.dataroute.GroupedDataSource
 */
@Aspect
@Component
public class DataRouteAspect extends BaseAspect {

	@Autowired(required = false)
	private RoutingDataSource routingDataSource;

	public DataRouteAspect() {
		order = -2;
	}

	@Around("execution(public * *(..)) and @annotation(transactional)")
	public Object determineReadonly(ProceedingJoinPoint jp, Transactional transactional) throws Throwable {
		if (routingDataSource == null)
			return jp.proceed();
		DataRouteContext.setReadonly(transactional.readOnly());
		try {
			return jp.proceed();
		} finally {
			DataRouteContext.removeReadonly();
		}
	}

	@Around("execution(public * *(..)) and @annotation(dataRoute)")
	public Object routeOnMethod(ProceedingJoinPoint jp, DataRoute dataRoute) throws Throwable {
		if (routingDataSource == null)
			return jp.proceed();
		Map<String, Object> context = buildContext(jp);
		String routingKey = ExpressionUtils.evalString(dataRoute.routingKey(), context);
		if (StringUtils.isNotBlank(routingKey)) {
			DataRouteContext.setRoutingKey(routingKey);
		} else {
			String nodeName = ExpressionUtils.evalString(dataRoute.nodeName(), context);
			if (StringUtils.isNotBlank(nodeName))
				DataRouteContext.setNodeName(nodeName);
		}
		return jp.proceed();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Around("execution(public * *(..)) and target(baseManager) and @annotation(transactional)")
	public Object routeOnClass(ProceedingJoinPoint jp, BaseManager baseManager, Transactional transactional)
			throws Throwable {
		if (routingDataSource == null)
			return jp.proceed();
		DataRoute dataRoute = null;
		Object target = jp.getTarget();
		if (target != null)
			dataRoute = target.getClass().getAnnotation(DataRoute.class);
		if (dataRoute == null) {
			Class<Persistable<?>> entityClass = baseManager.getEntityClass();
			if (entityClass != null)
				dataRoute = entityClass.getAnnotation(DataRoute.class);
		}
		if (dataRoute != null) {
			Map<String, Object> context = buildContext(jp);
			String nodeName = ExpressionUtils.evalString(dataRoute.nodeName(), context);
			if (StringUtils.isNotBlank(nodeName))
				DataRouteContext.setNodeName(nodeName);
		}
		return jp.proceed();
	}

}
