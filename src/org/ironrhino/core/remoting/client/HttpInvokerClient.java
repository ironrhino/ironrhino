package org.ironrhino.core.remoting.client;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.remoting.GenericRemoteInvocation;
import org.ironrhino.core.remoting.RemotingContext;
import org.ironrhino.core.remoting.SerializationType;
import org.ironrhino.core.remoting.ServiceRegistry;
import org.ironrhino.core.remoting.ServiceStats;
import org.ironrhino.core.spring.RemotingClientProxy;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.CodecUtils;
import org.ironrhino.core.util.ExceptionUtils;
import org.ironrhino.core.util.JsonDesensitizer;
import org.ironrhino.core.util.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.httpinvoker.HttpInvokerClientInterceptor;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import lombok.Getter;
import lombok.Setter;

public class HttpInvokerClient extends HttpInvokerClientInterceptor implements FactoryBean<Object> {

	private static final boolean resilience4jPresent = ClassUtils.isPresent(
			"io.github.resilience4j.circuitbreaker.CircuitBreaker", HttpInvokerClient.class.getClassLoader());

	private static final String SERVLET_PATH_PREFIX = "/remoting/httpinvoker/";

	private static Logger logger = LoggerFactory.getLogger(HttpInvokerClient.class);

	private static Logger remotingLogger = LoggerFactory.getLogger("remoting");

	@Getter
	@Value("${httpInvoker.serialization.type:JAVA}")
	private volatile SerializationType serializationType = SerializationType.JAVA;

	@Value("${httpInvoker.loggingPayload:true}")
	private boolean loggingPayload;

	@Setter
	@Autowired(required = false)
	private ServiceRegistry serviceRegistry;

	@Setter
	@Autowired(required = false)
	private ServiceStats serviceStats;

	@Setter
	private String host;

	@Setter
	private int port;

	@Setter
	private String contextPath;

	@Setter
	private int maxAttempts = 3;

	@Setter
	@Value("${httpInvoker.polling:false}")
	private boolean polling;

	private boolean urlFromDiscovery;

	private boolean discovered; // for lazy discover from serviceRegistry

	private String discoveredHost;

	private Object serviceProxy;

	@Value("${httpInvoker.circuitBreakerEnabled:true}")
	private boolean circuitBreakerEnabled = true;

	@Getter
	private Object circuitBreaker;

	@Override
	public Object getObject() {
		return this.serviceProxy;
	}

	@Override
	public Class<?> getObjectType() {
		return getServiceInterface();
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	public void setSerializationType(SerializationType serializationType) {
		this.serializationType = serializationType;
		SimpleHttpInvokerRequestExecutor executor = new SimpleHttpInvokerRequestExecutor(serializationType);
		executor.setBeanClassLoader(getBeanClassLoader());
		super.setHttpInvokerRequestExecutor(executor);
	}

	@Override
	public void afterPropertiesSet() {
		if (StringUtils.isBlank(host))
			Assert.notNull(serviceRegistry, "ServiceRegistry is missing");
		if (port <= 0)
			port = AppInfo.getHttpPort();
		if (port <= 0)
			port = 8080;
		String serviceUrl = getServiceUrl();
		if (serviceUrl == null) {
			Assert.notNull(serviceRegistry, "serviceRegistry shouldn't be null");
			setServiceUrl("http://fakehost/");
			discovered = false;
			urlFromDiscovery = true;
		}
		setSerializationType(getSerializationType());
		super.afterPropertiesSet();
		ProxyFactory pf = new ProxyFactory(getServiceInterface(), this);
		pf.addInterface(RemotingClientProxy.class);
		this.serviceProxy = pf.getProxy(getBeanClassLoader());
		if (circuitBreakerEnabled && resilience4jPresent) {
			io.github.resilience4j.circuitbreaker.CircuitBreakerConfig config = io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
					.custom().recordFailure(ex -> ex instanceof IOException).build();
			circuitBreaker = io.github.resilience4j.circuitbreaker.CircuitBreaker.of(getServiceInterface().getName(),
					config);
		}
	}

	@Override
	protected RemoteInvocation createRemoteInvocation(MethodInvocation methodInvocation) {
		if (getSerializationType() == SerializationType.JSON)
			return new GenericRemoteInvocation(methodInvocation);
		return super.createRemoteInvocation(methodInvocation);
	}

	@Override
	protected RemoteInvocationResult executeRequest(RemoteInvocation invocation, MethodInvocation methodInvocation)
			throws Exception {
		if (circuitBreaker == null)
			return doExecuteRequest(invocation, methodInvocation);
		else
			return ((io.github.resilience4j.circuitbreaker.CircuitBreaker) circuitBreaker)
					.executeCallable(() -> doExecuteRequest(invocation, methodInvocation));
	}

	protected RemoteInvocationResult doExecuteRequest(RemoteInvocation invocation, MethodInvocation methodInvocation)
			throws Exception {
		if (!discovered) {
			setServiceUrl(discoverServiceUrl());
			discovered = true;
		} else if (polling) {
			setServiceUrl(discoverServiceUrl(true));
		}
		String requestId = MDC.get("requestId");
		if (requestId == null) {
			requestId = CodecUtils.nextId();
			MDC.put("requestId", requestId);
			MDC.put("request", "request:" + requestId);
		}
		MDC.put("role", "CLIENT");
		MDC.put("service", getServiceInterface().getName() + '.' + stringify(methodInvocation));
		if (loggingPayload) {
			Object payload;
			Object[] arguments = methodInvocation.getArguments();
			String[] parameterNames = ReflectionUtils.getParameterNames(methodInvocation.getMethod());
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
		long time = System.currentTimeMillis();
		RemoteInvocationResult result;
		try {
			result = doExecuteRequest(invocation, methodInvocation, maxAttempts);
			time = System.currentTimeMillis() - time;
			if (loggingPayload) {
				if (!result.hasInvocationTargetException())
					remotingLogger.info("Response: {}", JsonDesensitizer.DEFAULT_INSTANCE.toJson(result.getValue()));
				else
					remotingLogger.error("Error:",
							((InvocationTargetException) result.getException()).getTargetException());
			}
			remotingLogger.info("Invoked to {} success in {}ms", discoveredHost, time);
		} finally {
			RemotingContext.clear();
			MDC.remove("role");
		}

		return result;
	}

	protected RemoteInvocationResult doExecuteRequest(RemoteInvocation invocation, MethodInvocation methodInvocation,
			int maxAttempts) throws Exception {
		String method = null;
		if (serviceStats != null)
			method = stringify(methodInvocation);
		int attempts = maxAttempts;
		while (attempts > 0) {
			long time = System.currentTimeMillis();
			try {
				RemoteInvocationResult result = super.executeRequest(invocation);
				if (serviceStats != null) {
					serviceStats.clientSideEmit(discoveredHost, getServiceInterface().getName(), method,
							System.currentTimeMillis() - time, false);
				}
				return result;
			} catch (Exception e) {
				remotingLogger.error("Exception:", e.getCause() != null ? e.getCause() : e);
				remotingLogger.info("Invoked to {} fail in {}ms", discoveredHost, System.currentTimeMillis() - time);
				if (serviceStats != null) {
					serviceStats.clientSideEmit(discoveredHost, getServiceInterface().getName(), method,
							System.currentTimeMillis() - time, true);
				}
				if (--attempts < 1)
					throw e;
				if ((e instanceof SerializationFailedException) && getSerializationType() != SerializationType.JAVA) {
					logger.error("Downgrade service[{}] serialization from {} to {}: {}",
							getServiceInterface().getName(), serializationType, SerializationType.JAVA, e.getMessage());
					setSerializationType(SerializationType.JAVA);
				} else {
					if (urlFromDiscovery) {
						if (discoveredHost != null) {
							serviceRegistry.evict(discoveredHost);
							discoveredHost = null;
						}
						String serviceUrl = discoverServiceUrl();
						if (!serviceUrl.equals(getServiceUrl())) {
							setServiceUrl(serviceUrl);
							logger.info("Relocate service url " + serviceUrl);
						}
					}
				}
			}
		}
		throw new IllegalArgumentException("maxAttempts should large than 0");
	}

	@Override
	protected RemoteAccessException convertHttpInvokerAccessException(Throwable ex) {
		RemoteAccessException rae = super.convertHttpInvokerAccessException(ex);
		if (rae.getCause() != null)
			ExceptionUtils.trimStackTrace(rae.getCause(), 20);
		ExceptionUtils.trimStackTrace(rae, 10);
		return rae;
	}

	@Override
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

	@Override
	public String getServiceUrl() {
		String serviceUrl = super.getServiceUrl();
		if (serviceUrl == null && StringUtils.isNotBlank(host)) {
			serviceUrl = discoverServiceUrl();
			setServiceUrl(serviceUrl);
		}
		return serviceUrl;
	}

	protected String discoverServiceUrl() {
		return discoverServiceUrl(false);
	}

	protected String discoverServiceUrl(boolean polling) {
		String serviceName = getServiceInterface().getName();
		StringBuilder sb = new StringBuilder();
		if (StringUtils.isBlank(host)) {
			String ho = serviceRegistry.discover(serviceName, polling);
			if (ho.indexOf("://") < 0)
				sb.append("http://");
			sb.append(ho);
			discoveredHost = ho;
		} else {
			sb.append("http://");
			sb.append(host);
			if (port != 80) {
				sb.append(':');
				sb.append(port);
			}
			if (StringUtils.isNotBlank(contextPath))
				sb.append(contextPath);
		}
		sb.append(SERVLET_PATH_PREFIX);
		sb.append(serviceName);
		return sb.toString();
	}

	private static String stringify(MethodInvocation methodInvocation) {
		StringBuilder method = new StringBuilder(methodInvocation.getMethod().getName()).append("(");
		Class<?>[] parameterTypes = methodInvocation.getMethod().getParameterTypes();
		for (int i = 0; i < parameterTypes.length; i++) {
			method.append(parameterTypes[i].getSimpleName());
			if (i < parameterTypes.length - 1)
				method.append(',');
		}
		method.append(")");
		return method.toString();
	}
}
