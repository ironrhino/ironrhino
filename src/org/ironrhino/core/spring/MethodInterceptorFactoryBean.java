package org.ironrhino.core.spring;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.ironrhino.core.util.NameableThreadFactory;
import org.ironrhino.core.util.ReflectionUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import lombok.Getter;
import lombok.Setter;

public abstract class MethodInterceptorFactoryBean
		implements MethodInterceptor, FactoryBean<Object>, DisposableBean, ApplicationContextAware {

	public static final String EXECUTOR_POOL_SIZE_SUFFIX = ".executor.pool.size";

	@Setter
	private volatile ExecutorService executorService;

	private boolean executorServiceCreated;

	@Getter
	@Setter
	private ApplicationContext applicationContext;

	@Override
	public Object invoke(final MethodInvocation methodInvocation) throws Throwable {
		Method method = methodInvocation.getMethod();
		if (AopUtils.isToStringMethod(methodInvocation.getMethod()))
			return "Dynamic proxy for [" + getObjectType().getName() + "]";
		if (method.isDefault())
			return ReflectionUtils.invokeDefaultMethod(getObject(), method, methodInvocation.getArguments());
		Class<?> returnType = method.getReturnType();
		if (returnType == Future.class) {
			return getExecutorService().submit(new Callable<Object>() {
				@Override
				public Object call() throws Exception {
					try {
						return doInvoke(methodInvocation);
					} catch (Exception e) {
						throw e;
					} catch (Throwable e) {
						throw new InvocationTargetException(e);
					}
				}
			});
		} else if (returnType == Callable.class) {
			return new Callable<Object>() {
				@Override
				public Object call() throws Exception {
					try {
						return doInvoke(methodInvocation);
					} catch (Exception e) {
						throw e;
					} catch (Throwable e) {
						throw new InvocationTargetException(e);
					}
				}
			};
		}
		return doInvoke(methodInvocation);
	}

	protected abstract Object doInvoke(MethodInvocation methodInvocation) throws Throwable;

	private ExecutorService getExecutorService() {
		ExecutorService es = executorService;
		if (es == null) {
			synchronized (this) {
				es = executorService;
				if (es == null) {
					String poolName = getObjectType().getSimpleName();
					ApplicationContext ctx = getApplicationContext();
					if (ctx != null) {
						int threads = ctx.getEnvironment()
								.getProperty(getObjectType().getName() + EXECUTOR_POOL_SIZE_SUFFIX, int.class, 5);
						executorService = es = Executors.newFixedThreadPool(threads,
								new NameableThreadFactory(poolName));
					} else {
						executorService = es = Executors.newCachedThreadPool(new NameableThreadFactory(poolName));
					}
					executorServiceCreated = true;
				}
			}
		}
		return es;
	}

	@Override
	public void destroy() {
		if (executorServiceCreated) {
			executorService.shutdown();
			executorServiceCreated = false;
		}
	}

}
