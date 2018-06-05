package org.ironrhino.core.cache;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.aop.AbstractMethodInterceptor;
import org.ironrhino.core.model.NullObject;
import org.ironrhino.core.util.ExpressionUtils;
import org.mvel2.PropertyAccessException;
import org.springframework.core.BridgeMethodResolver;

import lombok.Setter;

public class CacheInterceptor extends AbstractMethodInterceptor<CacheAspect> {

	private final static String MUTEX = "_MUTEX_";

	@Setter
	private CacheManager cacheManager;

	@Setter
	private boolean mutex;

	@Setter
	private int mutexWait;

	@Override
	public Object invoke(MethodInvocation methodInvocation) throws Throwable {
		Method method = methodInvocation.getMethod();
		method = BridgeMethodResolver.findBridgedMethod(method);
		CheckCache checkCache = method.getAnnotation(CheckCache.class);
		if (checkCache != null) {
			if (isBypass())
				return methodInvocation.proceed();
			Map<String, Object> context = buildContext(methodInvocation);
			String namespace = ExpressionUtils.evalString(checkCache.namespace(), context);
			List<String> keys = ExpressionUtils.evalList(checkCache.key(), context);
			if (keys == null || keys.isEmpty())
				return methodInvocation.proceed();
			String keyMutex = MUTEX + String.join("_", keys);
			boolean mutexed = false;
			if (CacheContext.isForceFlush()) {
				cacheManager.mdelete(new HashSet<>(keys), namespace);
			} else {
				Class<?> returnType = methodInvocation.getMethod().getReturnType();
				int timeToIdle = ExpressionUtils.evalInt(checkCache.timeToIdle(), context, 0);
				for (String key : keys) {
					Object value = (timeToIdle > 0 && !cacheManager.supportsTti())
							? cacheManager.getWithTti(key, namespace, timeToIdle, checkCache.timeUnit())
							: cacheManager.get(key, namespace);
					if (value instanceof NullObject) {
						ExpressionUtils.eval(checkCache.onHit(), context);
						return null;
					}
					if (value != null && (returnType.isPrimitive()
							&& value.getClass() == ClassUtils.primitiveToWrapper(returnType)
							|| returnType.isAssignableFrom(value.getClass()))) {
						putReturnValueIntoContext(context, value);
						ExpressionUtils.eval(checkCache.onHit(), context);
						return value;
					}
				}
				if (mutex) {
					int throughPermits = checkCache.throughPermits();
					if (cacheManager.increment(keyMutex, 1, Math.max(10000, mutexWait), TimeUnit.MILLISECONDS,
							namespace) <= throughPermits) {
						mutexed = true;
					} else {
						Thread.sleep(mutexWait);
						for (String key : keys) {
							Object value = cacheManager.get(key, namespace);
							if (value instanceof NullObject) {
								ExpressionUtils.eval(checkCache.onHit(), context);
								return null;
							}
							if (value != null && (returnType.isPrimitive()
									&& value.getClass() == ClassUtils.primitiveToWrapper(returnType)
									|| returnType.isAssignableFrom(value.getClass()))) {
								putReturnValueIntoContext(context, value);
								ExpressionUtils.eval(checkCache.onHit(), context);
								return value;
							}
						}
					}
				}
				ExpressionUtils.eval(checkCache.onMiss(), context);
			}
			Object result = methodInvocation.proceed();
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
				cacheManager.delete(keyMutex, namespace);
			return result;

		}
		EvictCache evictCache = method.getAnnotation(EvictCache.class);
		if (evictCache != null) {
			Map<String, Object> context = buildContext(methodInvocation);
			String namespace = ExpressionUtils.evalString(evictCache.namespace(), context);
			boolean fallback = false;
			List<String> keys = null;
			try {
				keys = ExpressionUtils.evalList(evictCache.key(), context);
			} catch (PropertyAccessException e) {
				fallback = true; // required retval
			}
			Object retval = methodInvocation.proceed();
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
		return methodInvocation.proceed();
	}

}
