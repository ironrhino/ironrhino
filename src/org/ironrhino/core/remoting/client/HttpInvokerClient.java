package org.ironrhino.core.remoting.client;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.remoting.RemotingContext;
import org.ironrhino.core.remoting.ServiceRegistry;
import org.ironrhino.core.remoting.serializer.HttpInvokerSerializer;
import org.ironrhino.core.remoting.serializer.HttpInvokerSerializers;
import org.ironrhino.core.remoting.stats.ServiceStats;
import org.ironrhino.core.spring.RemotingClientProxy;
import org.ironrhino.core.throttle.CircuitBreaking;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.CodecUtils;
import org.ironrhino.core.util.ExceptionUtils;
import org.ironrhino.core.util.JsonDesensitizer;
import org.ironrhino.core.util.NameableThreadFactory;
import org.ironrhino.core.util.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.lang.Nullable;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.httpinvoker.HttpInvokerClientInterceptor;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;
import org.springframework.util.Assert;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HttpInvokerClient extends HttpInvokerClientInterceptor implements FactoryBean<Object> {

	private static final String SERVLET_PATH_PREFIX = "/remoting/httpinvoker/";

	private static Logger remotingLogger = LoggerFactory.getLogger("remoting");

	@Getter
	@Setter
	@Value("${httpInvoker.connectTimeout:5000}")
	private int connectTimeout = 5000;

	@Getter
	@Setter
	@Value("${httpInvoker.readTimeout:60000}")
	private int readTimeout = 60000;

	@Getter
	private volatile HttpInvokerSerializer serializer;

	@Value("${httpInvoker.loggingPayload:true}")
	private boolean loggingPayload;

	@Setter
	@Autowired(required = false)
	private ServiceRegistry serviceRegistry;

	@Setter
	@Autowired(required = false)
	private ServiceStats serviceStats;

	@Setter
	@Autowired(required = false)
	private volatile ExecutorService executorService;

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

	@Override
	public Object getObject() {
		return this.serviceProxy;
	}

	@Override
	public Class<?> getObjectType() {
		return getServiceInterface();
	}

	@Value("${httpInvoker.serialization.type:}")
	public void setSerializationType(String serializationType) {
		this.serializer = HttpInvokerSerializers.ofSerializationType(serializationType);
		SimpleHttpInvokerRequestExecutor executor = new SimpleHttpInvokerRequestExecutor(this.serializer);
		executor.setConnectTimeout(connectTimeout);
		executor.setReadTimeout(readTimeout);
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
		super.afterPropertiesSet();
		ProxyFactory pf = new ProxyFactory(getServiceInterface(), this);
		pf.addInterface(RemotingClientProxy.class);
		this.serviceProxy = pf.getProxy(getBeanClassLoader());
	}

	@Override
	public Object invoke(MethodInvocation methodInvocation) throws Throwable {
		Method method = methodInvocation.getMethod();
		if (method.isDefault()) {
			return ReflectionUtils.invokeDefaultMethod(getObject(), method, methodInvocation.getArguments());
		}
		Class<?> returnType = method.getReturnType();
		if (returnType == Future.class) {
			ExecutorService es = executorService;
			if (es == null) {
				synchronized (this) {
					es = executorService;
					if (es == null)
						executorService = es = Executors
								.newCachedThreadPool(new NameableThreadFactory("httpInvokerClient"));
				}
			}
			return es.submit(new Callable<Object>() {
				@Override
				public Object call() throws Exception {
					try {
						return HttpInvokerClient.super.invoke(methodInvocation);
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
						return HttpInvokerClient.super.invoke(methodInvocation);
					} catch (Exception e) {
						throw e;
					} catch (Throwable e) {
						throw new InvocationTargetException(e);
					}
				}
			};
		}
		return super.invoke(methodInvocation);
	}

	@Override
	protected RemoteInvocation createRemoteInvocation(MethodInvocation methodInvocation) {
		return serializer.createRemoteInvocation(methodInvocation);
	}

	@Override
	protected RemoteInvocationResult executeRequest(RemoteInvocation invocation, MethodInvocation methodInvocation)
			throws Exception {
		return CircuitBreaking.execute(getServiceInterface().getName(), ex -> ex instanceof IOException,
				() -> doExecuteRequest(invocation, methodInvocation));
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
		String service = ReflectionUtils.stringify(methodInvocation.getMethod());
		MDC.put("role", "CLIENT");
		MDC.put("service", service);
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
			result = transformResult(invocation, methodInvocation, result);
			time = System.currentTimeMillis() - time;
			if (loggingPayload) {
				if (!result.hasInvocationTargetException()) {
					remotingLogger.info("Response: {}", JsonDesensitizer.DEFAULT_INSTANCE.toJson(result.getValue()));
				} else {
					InvocationTargetException ite = (InvocationTargetException) result.getException();
					if (ite != null)
						remotingLogger.error("Error:", ite.getTargetException());
				}
			}
			remotingLogger.info("Invoked to {} success in {}ms", discoveredHost, time);
		} finally {
			RemotingContext.clear();
			MDC.remove("role");
		}

		return result;
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

	protected RemoteInvocationResult doExecuteRequest(RemoteInvocation invocation, MethodInvocation methodInvocation,
			int maxAttempts) throws Exception {
		String method = null;
		if (serviceStats != null)
			method = ReflectionUtils.stringify(methodInvocation.getMethod(), false, true);
		int attempts = maxAttempts;
		do {
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
				if (attempts <= 1)
					throw e;
				if ((e instanceof SerializationFailedException)
						&& !serializer.equals(HttpInvokerSerializers.DEFAULT_SERIALIZER)) {
					log.error("Downgrade service[{}] serialization from {} to {}: {}", getServiceInterface().getName(),
							serializer.getSerializationType(),
							HttpInvokerSerializers.DEFAULT_SERIALIZER.getSerializationType(), e.getMessage());
					setSerializationType(HttpInvokerSerializers.DEFAULT_SERIALIZER.getSerializationType());
					RemoteInvocation newInvocation = HttpInvokerSerializers.DEFAULT_SERIALIZER
							.createRemoteInvocation(methodInvocation);
					newInvocation.setAttributes(invocation.getAttributes());
					invocation = newInvocation;
				} else {
					if (urlFromDiscovery) {
						if (discoveredHost != null) {
							serviceRegistry.evict(discoveredHost);
							discoveredHost = null;
						}
						String serviceUrl = discoverServiceUrl();
						if (!serviceUrl.equals(getServiceUrl())) {
							setServiceUrl(serviceUrl);
							log.info("Relocate service url " + serviceUrl);
						}
					}
				}
			}
		} while (--attempts > 0);
		throw new IllegalStateException("Should never happens");
	}

	@Override
	protected RemoteAccessException convertHttpInvokerAccessException(Throwable ex) {
		RemoteAccessException rae = super.convertHttpInvokerAccessException(ex);
		if (rae != null) {
			if (rae.getCause() != null)
				ExceptionUtils.trimStackTrace(rae.getCause(), 20);
			ExceptionUtils.trimStackTrace(rae, 10);
		}
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
	@Nullable
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

}
