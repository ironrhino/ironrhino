package org.ironrhino.core.spring;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import javax.validation.executable.ExecutableValidator;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.ironrhino.core.util.NameableThreadFactory;
import org.ironrhino.core.util.ReflectionUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureTask;
import org.springframework.validation.annotation.Validated;

import lombok.Getter;
import lombok.Setter;

public abstract class MethodInterceptorFactoryBean implements MethodInterceptor, FactoryBean<Object> {

	public static final int EXECUTOR_POOL_SIZE_DEFAULT = 5;

	public static final String EXECUTOR_BEAN_NAME_SUFFIX = ".executor.bean.name";

	public static final String EXECUTOR_POOL_SIZE_SUFFIX = ".executor.pool.size";

	@Setter
	private volatile ExecutorService executorService;

	private boolean executorServiceCreated;

	@Getter
	@Autowired
	private ApplicationContext applicationContext;

	@Autowired(required = false)
	private Validator validator;

	@Override
	public Object invoke(final MethodInvocation methodInvocation) throws Throwable {
		Method method = methodInvocation.getMethod();
		if (AopUtils.isToStringMethod(methodInvocation.getMethod())) {
			Class<?> objectType = getObjectType();
			return "Dynamic proxy for [" + (objectType != null ? objectType.getName() : "Unknown") + "]";
		}
		Object bean = getObject();
		ExecutableValidator executableValidator = null;
		Class<?>[] groups = null;
		if (validator != null) {
			executableValidator = validator.forExecutables();
			groups = determineValidationGroups(method);
		}
		if (executableValidator != null) {
			Set<ConstraintViolation<Object>> constraintViolations = executableValidator.validateParameters(bean, method,
					methodInvocation.getArguments(), groups);
			if (!constraintViolations.isEmpty())
				throw new ConstraintViolationException(constraintViolations);
		}
		if (method.isDefault())
			return ReflectionUtils.invokeDefaultMethod(bean, method, methodInvocation.getArguments());
		Class<?> returnType = method.getReturnType();
		if (returnType == Callable.class || returnType == ListenableFuture.class || returnType == Future.class) {
			Callable<Object> callable = new Callable<Object>() {
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
			if (returnType == Callable.class) {
				return callable;
			}
			if (returnType == ListenableFuture.class) {
				ListenableFutureTask<Object> future = new ListenableFutureTask<>(callable);
				getExecutorService().execute(future);
				return future;
			}
			if (returnType == Future.class) {
				return getExecutorService().submit(callable);
			}
		}
		Object returnValue = doInvoke(methodInvocation);
		if (executableValidator != null) {
			Set<ConstraintViolation<Object>> constraintViolations = executableValidator.validateReturnValue(bean,
					method, returnValue, groups);
			if (!constraintViolations.isEmpty())
				throw new ConstraintViolationException(constraintViolations);
		}
		return returnValue;
	}

	protected abstract Object doInvoke(MethodInvocation methodInvocation) throws Throwable;

	protected Class<?>[] determineValidationGroups(Method method) {
		Validated validatedAnn = AnnotationUtils.findAnnotation(method, Validated.class);
		if (validatedAnn == null)
			validatedAnn = AnnotationUtils.findAnnotation(method.getDeclaringClass(), Validated.class);
		return (validatedAnn != null ? validatedAnn.value() : new Class<?>[0]);
	}

	private ExecutorService getExecutorService() {
		ExecutorService es = executorService;
		if (es == null) {
			synchronized (this) {
				es = executorService;
				if (es == null) {
					Class<?> objectType = getObjectType();
					if (objectType == null)
						throw new RuntimeException("Unexpected null");
					String poolName = objectType.getSimpleName();
					if (applicationContext != null) {
						int threads = applicationContext.getEnvironment().getProperty(
								objectType.getName() + EXECUTOR_POOL_SIZE_SUFFIX, int.class,
								EXECUTOR_POOL_SIZE_DEFAULT);
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

	@PostConstruct
	private void init() {
		Class<?> objectType = getObjectType();
		if (objectType == null)
			throw new RuntimeException("Unexpected null");
		String executorBeanName = applicationContext.getEnvironment()
				.getProperty(objectType.getName() + EXECUTOR_BEAN_NAME_SUFFIX);
		if (executorBeanName != null)
			executorService = applicationContext.getBean(executorBeanName, ExecutorService.class);
	}

	@PreDestroy
	private void destroy() {
		if (executorServiceCreated) {
			executorService.shutdown();
			executorServiceCreated = false;
		}
	}

}
