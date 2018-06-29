package org.ironrhino.core.cache;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.ironrhino.core.aop.BaseAspect;
import org.ironrhino.core.metrics.Metrics;
import org.ironrhino.core.model.NullObject;
import org.ironrhino.core.util.ExpressionUtils;
import org.mvel2.PropertyAccessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class CacheAspect extends BaseAspect {

	private final static String MUTEX = "_MUTEX_";

	@Autowired
	private CacheManager cacheManager;

	public CacheAspect() {
		order = Ordered.HIGHEST_PRECEDENCE + 3;
	}

	@Around("execution(public * *(..)) and @annotation(checkCache)")
	public Object get(ProceedingJoinPoint jp, CheckCache checkCache) throws Throwable {
		if (isBypass())
			return jp.proceed();
		Map<String, Object> context = buildContext(jp);
		String namespace = ExpressionUtils.evalString(checkCache.namespace(), context);
		List<String> keys = ExpressionUtils.evalList(checkCache.key(), context);
		if (keys == null || keys.isEmpty())
			return jp.proceed();
		String keyMutex = MUTEX + String.join("_", keys);
		boolean mutexed = false;
		if (CacheContext.isForceFlush()) {
			cacheManager.mdelete(new HashSet<>(keys), namespace);
		} else {
			Class<?> returnType = ((MethodSignature) jp.getSignature()).getMethod().getReturnType();
			int timeToIdle = ExpressionUtils.evalInt(checkCache.timeToIdle(), context, 0);
			for (String key : keys) {
				Object value = (timeToIdle > 0 && !cacheManager.supportsTti())
						? cacheManager.getWithTti(key, namespace, timeToIdle, checkCache.timeUnit())
						: cacheManager.get(key, namespace);
				if (value instanceof NullObject) {
					ExpressionUtils.eval(checkCache.onHit(), context);
					instrument(namespace, true);
					return null;
				}
				if (value != null
						&& (returnType.isPrimitive() && value.getClass() == ClassUtils.primitiveToWrapper(returnType)
								|| returnType.isAssignableFrom(value.getClass()))) {
					putReturnValueIntoContext(context, value);
					ExpressionUtils.eval(checkCache.onHit(), context);
					instrument(namespace, true);
					return value;
				}
			}
			int throughPermits = checkCache.throughPermits();
			int waitTimeout = checkCache.waitTimeout();
			if (waitTimeout <= 0)
				waitTimeout = 200;
			else if (waitTimeout > 10000)
				waitTimeout = 10000;
			if (cacheManager.increment(keyMutex, 1, waitTimeout, TimeUnit.MILLISECONDS, namespace) <= throughPermits) {
				mutexed = true;
			} else {
				Thread.sleep(waitTimeout);
				for (String key : keys) {
					Object value = cacheManager.get(key, namespace);
					if (value instanceof NullObject) {
						ExpressionUtils.eval(checkCache.onHit(), context);
						instrument(namespace, true);
						return null;
					}
					if (value != null && (returnType.isPrimitive()
							&& value.getClass() == ClassUtils.primitiveToWrapper(returnType)
							|| returnType.isAssignableFrom(value.getClass()))) {
						putReturnValueIntoContext(context, value);
						ExpressionUtils.eval(checkCache.onHit(), context);
						instrument(namespace, true);
						return value;
					}
				}
			}
			ExpressionUtils.eval(checkCache.onMiss(), context);
			instrument(namespace, false);
		}
		Object result = jp.proceed();
		putReturnValueIntoContext(context, result);
		if (ExpressionUtils.evalBoolean(checkCache.when(), context, true)) {
			Object cacheResult = (result == null && checkCache.cacheNull()) ? NullObject.get() : result;
			if (cacheResult != null) {
				if (checkCache.eternal()) {
					for (String key : keys)
						cacheManager.put(key, cacheResult, 0, checkCache.timeUnit(), namespace);
				} else {
					int timeToLive = ExpressionUtils.evalInt(checkCache.timeToLive(), context, 0);
					int timeToIdle = ExpressionUtils.evalInt(checkCache.timeToIdle(), context, 0);
					for (String key : keys)
						if (timeToIdle > 0 && cacheManager.supportsTti())
							cacheManager.putWithTti(key, cacheResult, timeToIdle, checkCache.timeUnit(), namespace);
						else
							cacheManager.put(key, cacheResult, timeToLive, checkCache.timeUnit(), namespace);
				}
			}
			if (result != null)
				ExpressionUtils.eval(checkCache.onPut(), context);
		}
		if (mutexed)
			cacheManager.increment(keyMutex, -1, 0, TimeUnit.MILLISECONDS, namespace);
		return result;
	}

	@Around("execution(public * *(..)) and @annotation(evictCache)")
	public Object remove(ProceedingJoinPoint jp, EvictCache evictCache) throws Throwable {
		Map<String, Object> context = buildContext(jp);
		String namespace = ExpressionUtils.evalString(evictCache.namespace(), context);
		boolean fallback = false;
		List<String> keys = null;
		try {
			keys = ExpressionUtils.evalList(evictCache.key(), context);
		} catch (PropertyAccessException e) {
			fallback = true; // required retval
		}
		Object retval = jp.proceed();
		putReturnValueIntoContext(context, retval);
		if (fallback)
			keys = ExpressionUtils.evalList(evictCache.key(), context);
		if (isBypass() || keys == null || keys.size() == 0)
			return retval;
		cacheManager.mdelete(new HashSet<>(keys), namespace);
		ExpressionUtils.eval(evictCache.onEvict(), context);
		if (StringUtils.isNotBlank(evictCache.renew())) {
			Object value = ExpressionUtils.eval(evictCache.renew(), context);
			// keys may be changed, eval again
			if (!fallback)
				keys = ExpressionUtils.evalList(evictCache.key(), context);
			for (Object key : keys)
				if (key != null)
					cacheManager.put(key.toString(), value, 0, TimeUnit.SECONDS, namespace);
		}
		return retval;
	}

	private static void instrument(String namespace, boolean hit) {
		Metrics.increment("cache." + namespace, "hit", String.valueOf(hit));
	}

}
