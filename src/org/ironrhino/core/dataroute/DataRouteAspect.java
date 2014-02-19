package org.ironrhino.core.dataroute;

import static org.ironrhino.core.metadata.Profiles.CLUSTER;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.ironrhino.core.aop.BaseAspect;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.service.BaseManager;
import org.ironrhino.core.util.ExpressionUtils;
import org.springframework.context.annotation.Profile;
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
@Profile(CLUSTER)
public class DataRouteAspect extends BaseAspect {

	public DataRouteAspect() {
		order = -2;
	}

	@Around("execution(public * *(..)) and @annotation(transactional)")
	public Object determineReadonly(ProceedingJoinPoint jp,
			Transactional transactional) throws Throwable {
		if (transactional.readOnly())
			DataRouteContext.setReadonly(true);
		return jp.proceed();
	}

	@Around("execution(public * *(..)) and @annotation(dataRoute))")
	public Object determineGroup(ProceedingJoinPoint jp, DataRoute dataRoute)
			throws Throwable {
		Map<String, Object> context = buildContext(jp);
		String groupName = ExpressionUtils.evalString(dataRoute.value(),
				context);
		if (StringUtils.isNotBlank(groupName))
			DataRouteContext.setName(groupName);
		return jp.proceed();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Around("execution(public * *(..)) and target(baseManager)")
	public Object determineGroup(ProceedingJoinPoint jp, BaseManager baseManager)
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
		if (dataRoute != null) {
			Map<String, Object> context = buildContext(jp);
			String groupName = ExpressionUtils.evalString(dataRoute.value(),
					context);
			if (StringUtils.isNotBlank(groupName))
				DataRouteContext.setName(groupName);
		}
		return jp.proceed();
	}

}
