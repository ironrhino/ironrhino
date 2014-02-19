package org.ironrhino.core.cache;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.ironrhino.core.aop.BaseAspect;
import org.ironrhino.core.model.NullObject;
import org.ironrhino.core.util.ExpressionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class CacheAspect extends BaseAspect {

	private final static String MUTEX = "_MUTEX_";

	private final static int DEFAULT_MUTEX_WAIT = 200;

	@Autowired
	private CacheManager cacheManager;

	@Value("${cacheAspect.mutex:true}")
	private boolean mutex;

	@Value("${cacheAspect.mutexWait:" + DEFAULT_MUTEX_WAIT + "}")
	private int mutexWait = DEFAULT_MUTEX_WAIT;

	public CacheAspect() {
		order = -100;
	}

	@Around("execution(public * *(..)) and @annotation(checkCache)")
	public Object get(ProceedingJoinPoint jp, CheckCache checkCache)
			throws Throwable {
		Map<String, Object> context = buildContext(jp);
		String namespace = ExpressionUtils.evalString(checkCache.namespace(),
				context);
		String key = ExpressionUtils.evalString(checkCache.key(), context);
		if (key == null || isBypass())
			return jp.proceed();
		String keyMutex = MUTEX + key;
		boolean mutexed = false;
		if (CacheContext.isForceFlush()) {
			cacheManager.delete(key, namespace);
		} else {
			int timeToIdle = ExpressionUtils.evalInt(checkCache.timeToIdle(),
					context, 0);
			Object value = (timeToIdle > 0 && !cacheManager
					.supportsTimeToIdle()) ? cacheManager.get(key, namespace,
					timeToIdle, checkCache.timeUnit()) : cacheManager.get(key,
					namespace);
			if (value != null) {
				putReturnValueIntoContext(context, value);
				ExpressionUtils.eval(checkCache.onHit(), context);
				return value instanceof NullObject ? null : value;
			} else {
				if (mutex) {
					if (cacheManager.putIfAbsent(keyMutex, "",
							Math.max(10000, mutexWait), TimeUnit.MILLISECONDS,
							namespace)) {
						mutexed = true;
					} else {
						Thread.sleep(mutexWait);
						value = cacheManager.get(key, namespace);
						if (value != null) {
							putReturnValueIntoContext(context, value);
							ExpressionUtils.eval(checkCache.onHit(), context);
							return value instanceof NullObject ? null : value;
						}
					}
				}
				ExpressionUtils.eval(checkCache.onMiss(), context);
			}
		}
		Object result = jp.proceed();
		putReturnValueIntoContext(context, result);
		if (ExpressionUtils.evalBoolean(checkCache.when(), context, true)) {
			Object cacheResult = (result == null ? NullObject.get() : result);
			if (checkCache.eternal()) {
				cacheManager.put(key, cacheResult, 0, checkCache.timeUnit(),
						namespace);
			} else {
				int timeToLive = ExpressionUtils.evalInt(
						checkCache.timeToLive(), context, 0);
				int timeToIdle = ExpressionUtils.evalInt(
						checkCache.timeToIdle(), context, 0);
				cacheManager.put(key, cacheResult, timeToIdle, timeToLive,
						checkCache.timeUnit(), namespace);
			}
			if (result != null)
				ExpressionUtils.eval(checkCache.onPut(), context);
		}
		if (mutexed)
			cacheManager.delete(keyMutex, namespace);
		return result;
	}

	@SuppressWarnings("unchecked")
	@AfterReturning(pointcut = "@annotation(evictCache)", returning = "retval")
	public void remove(JoinPoint jp, EvictCache evictCache, Object retval) {
		Map<String, Object> context = buildContext(jp);
		putReturnValueIntoContext(context, retval);
		String namespace = ExpressionUtils.evalString(evictCache.namespace(),
				context);
		List<String> keys = ExpressionUtils.evalList(evictCache.key(), context);
		if (isBypass() || keys == null || keys.size() == 0)
			return;
		if (StringUtils.isNotBlank(evictCache.renew())) {
			Object value = ExpressionUtils.eval(evictCache.renew(), context);
			for (Object key : keys)
				if (key != null)
					cacheManager.put(key.toString(), value, 0,
							TimeUnit.SECONDS, namespace);
		} else {
			cacheManager.mdelete(keys, namespace);
		}
		ExpressionUtils.eval(evictCache.onEvict(), context);
	}

}
