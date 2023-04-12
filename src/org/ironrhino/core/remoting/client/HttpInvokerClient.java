package org.ironrhino.core.remoting.client;

import java.io.IOException;
import java.io.InvalidClassException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.remoting.RemoteAccessException;
import org.ironrhino.core.remoting.RemoteConnectFailureException;
import org.ironrhino.core.remoting.RemoteInvocationFailureException;
import org.ironrhino.core.remoting.RemoteLookupFailureException;
import org.ironrhino.core.remoting.Remoting;
import org.ironrhino.core.remoting.ServiceNotFoundException;
import org.ironrhino.core.remoting.ServiceRegistry;
import org.ironrhino.core.remoting.serializer.HttpInvokerSerializers;
import org.ironrhino.core.remoting.stats.ServiceStats;
import org.ironrhino.core.spring.FallbackSupportMethodInterceptorFactoryBean;
import org.ironrhino.core.spring.RemotingClientProxy;
import org.ironrhino.core.throttle.CircuitBreakerRegistry;
import org.ironrhino.core.tracing.Tracing;
import org.ironrhino.core.util.CodecUtils;
import org.ironrhino.core.util.ExceptionUtils;
import org.ironrhino.core.util.JsonDesensitizer;
import org.ironrhino.core.util.MaxAttemptsExceededException;
import org.ironrhino.core.util.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;
import org.springframework.util.Assert;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HttpInvokerClient extends FallbackSupportMethodInterceptorFactoryBean {

	public static final String BASE_URL_SUFFIX = ".serviceBaseUrl";

	public static final String SERIALIZATION_TYPE_SUFFIX = ".serializationType";

	private static final String SERVLET_PATH_PREFIX = "/remoting/httpinvoker/";

	private static Logger remotingLogger = LoggerFactory.getLogger("remoting");

	@Getter
	@Setter
	private Class<?> serviceInterface;

	private volatile String serviceUrl;

	@Getter
	@Setter
	@Autowired(required = false)
	private HttpInvokerRequestExecutor httpInvokerRequestExecutor = new SimpleHttpInvokerRequestExecutor();

	@Value("${httpInvoker.serializationType:}")
	private String serializationType;

	@Getter
	@Setter
	@Value("${httpInvoker.connectTimeout:5000}")
	private int connectTimeout = 5000;

	@Getter
	@Setter
	@Value("${httpInvoker.readTimeout:20000}")
	private int readTimeout = 20000;

	@Getter
	@Setter
	@Value("${httpInvoker.loggingPayload:true}")
	private boolean loggingPayload;

	@Autowired(required = false)
	private CircuitBreakerRegistry circuitBreakerRegistry;

	@Setter
	@Autowired(required = false)
	private ServiceRegistry serviceRegistry;

	@Setter
	@Autowired(required = false)
	private ServiceStats serviceStats;

	@Setter
	private String baseUrl;

	@Setter
	private int maxAttempts = 3;

	private boolean urlFromDiscovery;

	private volatile String discoveredHost;

	private Object serviceProxy;

	@Override
	public Object getObject() {
		return this.serviceProxy;
	}

	@Override
	public Class<?> getObjectType() {
		return this.serviceInterface;
	}

	@PostConstruct
	public void afterPropertiesSet() {
		Assert.notNull(serviceInterface, "'serviceInterface' must not be null");
		Assert.isTrue(serviceInterface.isInterface(), "'serviceInterface' must be an interface");
		Assert.notNull(httpInvokerRequestExecutor, "'httpInvokerRequestExecutor' must not be null");
		Remoting anno = serviceInterface.getAnnotation(Remoting.class);
		if (anno != null) {
			if (StringUtils.isNotBlank(anno.serializationType()))
				this.serializationType = anno.serializationType();
			if (anno.maxAttempts() > 0)
				this.maxAttempts = anno.maxAttempts();
		}
		if (getApplicationContext() != null) {
			Environment env = getApplicationContext().getEnvironment();
			this.serializationType = env.getProperty(serviceInterface.getName() + SERIALIZATION_TYPE_SUFFIX,
					this.serializationType);
			httpInvokerRequestExecutor.setSerializer(HttpInvokerSerializers.ofSerializationType(serializationType));
			httpInvokerRequestExecutor.setConnectTimeout(connectTimeout);
			httpInvokerRequestExecutor.setReadTimeout(readTimeout);
			if (StringUtils.isBlank(baseUrl)) {
				baseUrl = env.getProperty(serviceInterface.getName() + BASE_URL_SUFFIX);
				if (StringUtils.isNotBlank(baseUrl)) {
					baseUrl = env.resolvePlaceholders(baseUrl);
					log.info("Discover baseUrl \"{}\" for service {} from environment", baseUrl,
							serviceInterface.getName());
				}
			}
		}
		if (StringUtils.isBlank(baseUrl)) {
			Assert.notNull(serviceRegistry, "serviceRegistry is missing");
			urlFromDiscovery = true;
		}
		ProxyFactory pf = new ProxyFactory(serviceInterface, this);
		pf.addInterface(RemotingClientProxy.class);
		this.serviceProxy = pf.getProxy(serviceInterface.getClassLoader());
	}

	@Override
	protected boolean shouldFallBackFor(Throwable ex) {
		if (ex instanceof RemoteAccessException) {
			ex = ex.getCause();
			return ex.getClass().getName().equals("io.github.resilience4j.circuitbreaker.CallNotPermittedException")
					|| ex instanceof ServiceNotFoundException;
		}
		return false;
	}

	@Override
	protected Object doInvoke(MethodInvocation methodInvocation) throws Throwable {
		RemoteInvocation invocation = httpInvokerRequestExecutor.getSerializer()
				.createRemoteInvocation(methodInvocation);
		RemoteInvocationResult result;
		try {
			result = executeRequest(invocation, methodInvocation);
		} catch (Throwable ex) {
			RemoteAccessException rae = convertHttpInvokerAccessException(ex);
			throw (rae != null ? rae : ex);
		}
		try {
			return recreateRemoteInvocationResult(result);
		} catch (Throwable ex) {
			if (result.hasInvocationTargetException()) {
				throw ex;
			} else {
				throw new RemoteInvocationFailureException("Invocation of method [" + methodInvocation.getMethod()
						+ "] failed in HTTP invoker remote service at [" + serviceUrl + "]", ex);
			}
		}
	}

	protected RemoteInvocationResult executeRequest(RemoteInvocation invocation, MethodInvocation methodInvocation)
			throws Throwable {
		return circuitBreakerRegistry != null
				? circuitBreakerRegistry
						.of(ReflectionUtils.stringify(methodInvocation.getMethod()), ex -> ex instanceof IOException)
						.executeCheckedSupplier(() -> doExecuteRequest(invocation, methodInvocation))
				: doExecuteRequest(invocation, methodInvocation);
	}

	protected RemoteInvocationResult doExecuteRequest(RemoteInvocation invocation, MethodInvocation methodInvocation)
			throws Exception {
		boolean requestIdGenerated = CodecUtils.putRequestIdIfAbsent();
		Method method = methodInvocation.getMethod();
		MDC.put("role", "CLIENT");
		if (loggingPayload) {
			Object payload;
			Object[] arguments = methodInvocation.getArguments();
			String[] parameterNames = ReflectionUtils.getParameterNames(method);
			if (parameterNames != null) {
				Map<String, Object> parameters = new LinkedHashMap<>();
				for (int i = 0; i < parameterNames.length; i++)
					parameters.put(parameterNames[i], arguments[i]);
				payload = parameters;
			} else {
				payload = arguments;
			}
			remotingLogger.info("Request: {}", JsonDesensitizer.DEFAULT_INSTANCE.toJson(payload));
		}
		RemoteInvocationResult result;
		try {
			result = doExecuteRequest(invocation, methodInvocation, maxAttempts);
			result = transformResult(invocation, methodInvocation, result);
			if (loggingPayload) {
				if (!result.hasInvocationTargetException()) {
					remotingLogger.info("Response: {}", JsonDesensitizer.DEFAULT_INSTANCE.toJson(result.getValue()));
				} else {
					InvocationTargetException ite = (InvocationTargetException) result.getException();
					if (ite != null)
						remotingLogger.error("Error:", ite.getTargetException());
				}
			}

		} finally {
			if (requestIdGenerated) {
				CodecUtils.removeRequestId();
			}
		}
		return result;
	}

	protected RemoteInvocationResult doExecuteRequest(RemoteInvocation invocation, MethodInvocation methodInvocation,
			int maxAttempts) throws Exception {
		String method = null;
		if (serviceStats != null)
			method = ReflectionUtils.stringify(methodInvocation.getMethod(), false, true);
		int remainingAttempts = maxAttempts;
		do {
			String targetServiceUrl = discoverServiceUrl();
			String targetDiscoveredHost = discoveredHost;
			long time = System.nanoTime();
			try {
				RemoteInvocation actualInvocation = invocation;
				RemoteInvocationResult result = Tracing.executeCheckedCallable(
						ReflectionUtils.stringify(methodInvocation.getMethod()),
						() -> httpInvokerRequestExecutor.executeRequest(targetServiceUrl, actualInvocation,
								methodInvocation),
						"span.kind", "client", "component", "remoting", "peer.address", targetServiceUrl);
				if (urlFromDiscovery) {
					time = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - time);
					remotingLogger.info("Invoked to {} success in {}ms", targetDiscoveredHost, time);
					if (serviceStats != null) {
						serviceStats.clientSideEmit(targetDiscoveredHost, getServiceInterface().getName(), method, time,
								false);
					}
				}
				return result;
			} catch (Exception e) {
				remotingLogger.error("Exception:", e.getCause() != null ? e.getCause() : e);
				if (urlFromDiscovery) {
					time = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - time);
					remotingLogger.info("Invoked to {} fail in {}ms", targetDiscoveredHost, time);
					if (serviceStats != null) {
						serviceStats.clientSideEmit(targetDiscoveredHost, getServiceInterface().getName(), method, time,
								true);
					}
				}
				if (remainingAttempts <= 1)
					throw e;
				if ((e instanceof SerializationFailedException) && !httpInvokerRequestExecutor.getSerializer()
						.equals(HttpInvokerSerializers.DEFAULT_SERIALIZER)) {
					log.error("Downgrade service[{}] serialization from {} to {}: {}", getServiceInterface().getName(),
							httpInvokerRequestExecutor.getSerializer().getSerializationType(),
							HttpInvokerSerializers.DEFAULT_SERIALIZER.getSerializationType(), e.getMessage());
					httpInvokerRequestExecutor.setSerializer(HttpInvokerSerializers.DEFAULT_SERIALIZER);
					RemoteInvocation newInvocation = HttpInvokerSerializers.DEFAULT_SERIALIZER
							.createRemoteInvocation(methodInvocation);
					newInvocation.setAttributes(invocation.getAttributes());
					invocation = newInvocation;
				} else {
					if (urlFromDiscovery) {
						if (targetDiscoveredHost != null) {
							serviceRegistry.evict(targetDiscoveredHost);
						}
					}
				}
			}
		} while (--remainingAttempts > 0);
		throw new MaxAttemptsExceededException(maxAttempts);
	}

	protected RemoteAccessException convertHttpInvokerAccessException(Throwable ex) {
		RemoteAccessException rae = null;
		if (ex instanceof ConnectException) {
			rae = new RemoteConnectFailureException(
					"Could not connect to HTTP invoker remote service at [" + serviceUrl + "]", ex);
		} else if (ex instanceof ServiceNotFoundException) {
			rae = new RemoteLookupFailureException(
					"Could not found remote service [" + getServiceInterface().getName() + "]", ex);
		} else if (ex instanceof ClassNotFoundException || ex instanceof NoClassDefFoundError
				|| ex instanceof InvalidClassException) {
			rae = new RemoteAccessException(
					"Could not deserialize result from HTTP invoker remote service [" + serviceUrl + "]", ex);
		} else if (ex instanceof Exception) {
			rae = new RemoteAccessException("Could not access HTTP invoker remote service at [" + serviceUrl + "]", ex);
		}
		if (rae != null) {
			if (rae.getCause() != null)
				ExceptionUtils.trimStackTrace(rae.getCause(), 20);
			ExceptionUtils.trimStackTrace(rae, 10);
		}
		return rae;
	}

	protected RemoteInvocationResult transformResult(RemoteInvocation invocation, MethodInvocation methodInvocation,
			RemoteInvocationResult result) {
		if (!result.hasException()) {
			Object value = result.getValue();
			if (methodInvocation.getMethod().getReturnType() == Optional.class && !(value instanceof Optional)) {
				result.setValue(Optional.ofNullable(value));
			}
		}
		return result;
	}

	protected Object recreateRemoteInvocationResult(RemoteInvocationResult result) throws Throwable {
		Throwable exception = result.getException();
		if (exception != null) {
			Throwable exToThrow = exception;
			if (exToThrow instanceof InvocationTargetException)
				exToThrow = ((InvocationTargetException) exToThrow).getTargetException();
			ExceptionUtils.fillInClientStackTraceIfPossible(exToThrow, 20);
			throw exToThrow;
		}
		return result.getValue();
	}

	private String discoverServiceUrl() {
		String serviceName = getServiceInterface().getName();
		StringBuilder sb = new StringBuilder();
		if (StringUtils.isBlank(baseUrl)) {
			String ho = serviceRegistry.discover(serviceName);
			if (ho.indexOf("://") < 0)
				sb.append("http://");
			sb.append(ho);
			discoveredHost = ho;
		} else {
			sb.append(baseUrl);
		}
		sb.append(SERVLET_PATH_PREFIX);
		sb.append(serviceName);
		return serviceUrl = sb.toString();
	}

}
