package org.ironrhino.core.remoting.client;

import java.lang.reflect.InvocationTargetException;

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
import org.ironrhino.core.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.remoting.httpinvoker.HttpInvokerClientInterceptor;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;
import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.JsonMappingException;

public class HttpInvokerClient extends HttpInvokerClientInterceptor implements FactoryBean<Object> {

	private static final String SERVLET_PATH_PREFIX = "/remoting/httpinvoker/";

	private static Logger logger = LoggerFactory.getLogger(HttpInvokerClient.class);

	private static Logger remotingLogger = LoggerFactory.getLogger("remoting");

	@Value("${httpInvoker.serialization.type:JAVA}")
	private volatile SerializationType serializationType = SerializationType.JAVA;

	@Value("${httpInvoker.loggingPayload:false}")
	private boolean loggingPayload;

	@Autowired(required = false)
	private ServiceRegistry serviceRegistry;

	@Autowired(required = false)
	private ServiceStats serviceStats;

	@Value("${remoting.channel.secure:false}")
	private boolean secure;

	private String host;

	private int port;

	private String contextPath;

	private int maxAttempts = 3;

	@Value("${httpInvoker.poll:false}")
	private boolean poll;

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

	@Override
	public boolean isSingleton() {
		return true;
	}

	public void setPoll(boolean poll) {
		this.poll = poll;
	}

	public void setSecure(boolean secure) {
		this.secure = secure;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	public void setMaxAttempts(int maxAttempts) {
		this.maxAttempts = maxAttempts;
	}

	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	public void setServiceStats(ServiceStats serviceStat) {
		this.serviceStats = serviceStat;
	}

	public SerializationType getSerializationType() {
		return serializationType;
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
			Assert.notNull(serviceRegistry);
			setServiceUrl((secure ? "https" : "http") + "://fakehost/");
			discovered = false;
			urlFromDiscovery = true;
		}
		setSerializationType(getSerializationType());
		super.afterPropertiesSet();
		ProxyFactory pf = new ProxyFactory(getServiceInterface(), this);
		pf.addInterface(RemotingClientProxy.class);
		this.serviceProxy = pf.getProxy(getBeanClassLoader());
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
		if (!discovered) {
			setServiceUrl(discoverServiceUrl());
			discovered = true;
		} else if (poll) {
			setServiceUrl(discoverServiceUrl());
		}
		String requestId = MDC.get("requestId");
		if (requestId == null) {
			requestId = CodecUtils.nextId();
			MDC.put("requestId", requestId);
			MDC.put("request", "request:" + requestId);
		}
		MDC.put("role", "CLIENT");
		MDC.put("service", getServiceInterface().getName() + '.' + stringify(methodInvocation));
		if (loggingPayload)
			remotingLogger.info("Request: {}", JsonUtils.toJson(methodInvocation.getArguments()));
		long time = System.currentTimeMillis();
		RemoteInvocationResult result;
		try {
			result = executeRequest(invocation, methodInvocation, maxAttempts);
			time = System.currentTimeMillis() - time;
			if (loggingPayload) {
				if (!result.hasInvocationTargetException())
					remotingLogger.info("Response: {}", JsonUtils.toJson(result.getValue()));
				else
					remotingLogger.error("Error:\n",
							((InvocationTargetException) result.getException()).getTargetException());
			}
			remotingLogger.info("Invoked to {} success in {}ms", discoveredHost, time);
		} finally {
			RemotingContext.clear();
			MDC.remove("role");
		}

		return result;
	}

	protected RemoteInvocationResult executeRequest(RemoteInvocation invocation, MethodInvocation methodInvocation,
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
				remotingLogger.error("Exception:\n", e.getCause() != null ? e.getCause() : e);
				remotingLogger.info("Invoked to {} fail in {}ms", discoveredHost, System.currentTimeMillis() - time);
				if (serviceStats != null) {
					serviceStats.clientSideEmit(discoveredHost, getServiceInterface().getName(), method,
							System.currentTimeMillis() - time, true);
				}
				if (--attempts < 1)
					throw e;
				Throwable throwable = e.getCause();
				if (throwable instanceof SerializationFailedException
						&& getSerializationType() == SerializationType.FST) {
					setSerializationType(SerializationType.JAVA);
					logger.error("downgrade serialization from FST to JAVA for service[{}]: {}",
							getServiceInterface().getName(), throwable.getMessage());
				} else if (throwable instanceof JsonMappingException
						&& getSerializationType() == SerializationType.JSON) {
					setSerializationType(SerializationType.JAVA);
					logger.error("downgrade serialization from JSON to JAVA for service[{}]: {}",
							getServiceInterface().getName(), throwable.getMessage());
				} else {
					if (urlFromDiscovery) {
						if (discoveredHost != null) {
							serviceRegistry.evict(discoveredHost);
							discoveredHost = null;
						}
						String serviceUrl = discoverServiceUrl();
						if (!serviceUrl.equals(getServiceUrl())) {
							setServiceUrl(serviceUrl);
							logger.info("relocate service url " + serviceUrl);
						}
					}
				}
			}
		}
		throw new IllegalArgumentException("maxAttempts should large than 0");
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
		String serviceName = getServiceInterface().getName();
		StringBuilder sb = new StringBuilder(secure ? "https" : "http");
		sb.append("://");
		if (StringUtils.isBlank(host)) {
			String ho = serviceRegistry.discover(serviceName);
			if (ho != null) {
				sb.append(ho);
				discoveredHost = ho;
			} else {
				logger.error("couldn't discover service " + serviceName);
				throw new ServiceNotFoundException(serviceName);
			}
		} else {
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
