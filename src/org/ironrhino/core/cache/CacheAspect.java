package org.ironrhino.core.cache;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.ironrhino.core.aop.BaseAspect;
import org.ironrhino.core.model.NullObject;
import org.ironrhino.core.util.ExpressionUtils;
import org.mvel2.PropertyAccessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
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
		order = Ordered.HIGHEST_PRECEDENCE + 3;
	}

	@Around("execution(public * *(..)) and @annotation(checkCache)")
	public Object get(ProceedingJoinPoint jp, CheckCache checkCache) throws Throwable {
		Map<String, Object> context = buildContext(jp);
		String namespace = ExpressionUtils.evalString(checkCache.namespace(), context);
		String key = ExpressionUtils.evalString(checkCache.key(), context);
		if (key == null || isBypass())
			return jp.proceed();
		String keyMutex = MUTEX + key;
		boolean mutexed = false;
		if (CacheContext.isForceFlush()) {
			cacheManager.delete(key, namespace);
		} else {
			int timeToIdle = ExpressionUtils.evalInt(checkCache.timeToIdle(), context, 0);
			Object value = (timeToIdle > 0 && !cacheManager.supportsTimeToIdle())
					? cacheManager.get(key, namespace, timeToIdle, checkCache.timeUnit())
					: cacheManager.get(key, namespace);
			if (value != null) {
				putReturnValueIntoContext(context, value instanceof NullObject ? null : value);
				ExpressionUtils.eval(checkCache.onHit(), context);
				return value instanceof NullObject ? null : value;
			} else {
				if (mutex) {
					int throughPermits = checkCache.throughPermits();
					if (cacheManager.increment(keyMutex, 1, Math.max(10000, mutexWait), TimeUnit.MILLISECONDS,
							namespace) <= throughPermits) {
						mutexed = true;
					} else {
						Thread.sleep(mutexWait);
						value = cacheManager.get(key, namespace);
						if (value != null) {
							putReturnValueIntoContext(context, value instanceof NullObject ? null : value);
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
			Object cacheResult = (result == null && checkCache.cacheNull()) ? NullObject.get() : result;
			if (cacheResult != null) {
				if (checkCache.eternal()) {
					cacheManager.put(key, cacheResult, 0, checkCache.timeUnit(), namespace);
				} else {
					int timeToLive = ExpressionUtils.evalInt(checkCache.timeToLive(), context, 0);
					int timeToIdle = ExpressionUtils.evalInt(checkCache.timeToIdle(), context, 0);
					cacheManager.put(key, cacheResult, timeToIdle, timeToLive, checkCache.timeUnit(), namespace);
				}
			}
			if (result != null)
				ExpressionUtils.eval(checkCache.onPut(), context);
		}
		if (mutexed)
			cacheManager.delete(keyMutex, namespace);
		return result;
	}

	@SuppressWarnings("unchecked")
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
		cacheManager.mdelete(keys, namespace);
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

}
