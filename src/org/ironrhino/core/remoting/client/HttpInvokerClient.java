package org.ironrhino.core.remoting.client;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.remoting.RemotingContext;
import org.ironrhino.core.remoting.ServiceRegistry;
import org.ironrhino.core.remoting.ServiceStats;
import org.ironrhino.core.spring.RemotingClientProxy;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.JsonUtils;
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
import org.springframework.util.Assert;

public class HttpInvokerClient extends HttpInvokerClientInterceptor implements FactoryBean<Object> {

	private static final String SERVLET_PATH_PREFIX = "/remoting/httpinvoker/";

	private static Logger logger = LoggerFactory.getLogger(HttpInvokerClient.class);

	@Value("${httpInvoker.useFstSerialization:false}")
	private boolean useFstSerialization;

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

	public boolean isUseFstSerialization() {
		return useFstSerialization;
	}

	public void setUseFstSerialization(boolean useFstSerialization) {
		this.useFstSerialization = useFstSerialization;
		SimpleHttpInvokerRequestExecutor executor = useFstSerialization ? new FstSimpleHttpInvokerRequestExecutor()
				: new SimpleHttpInvokerRequestExecutor();
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
		setUseFstSerialization(this.useFstSerialization);
		super.afterPropertiesSet();
		ProxyFactory pf = new ProxyFactory(getServiceInterface(), this);
		pf.addInterface(RemotingClientProxy.class);
		this.serviceProxy = pf.getProxy(getBeanClassLoader());
	}

	@Override
	public Object invoke(final MethodInvocation invocation) throws Throwable {
		if (!discovered) {
			setServiceUrl(discoverServiceUrl());
			discovered = true;
		} else if (poll) {
			setServiceUrl(discoverServiceUrl());
		}
		if (loggingPayload) {
			logger.info("invoking {}.{}() with:\n{}", getServiceInterface().getName(), invocation.getMethod().getName(),
					JsonUtils.toJson(invocation.getArguments()));
		}
		long time = System.currentTimeMillis();
		Object value;
		try {
			value = invoke(invocation, maxAttempts);
		} finally {
			RemotingContext.clear();
		}
		time = System.currentTimeMillis() - time;
		if (loggingPayload) {
			MDC.remove("url");
			if (value != null) {
				logger.info("returned in {}ms:\n{}", time, JsonUtils.toJson(value));
			} else {
				logger.info("returned in {}ms: null", time);
			}
		}
		return value;
	}

	public Object invoke(MethodInvocation invocation, int attempts) throws Throwable {
		String method = null;
		if (serviceStats != null) {
			StringBuilder sb = new StringBuilder(invocation.getMethod().getName()).append("(");
			Class<?>[] parameterTypes = invocation.getMethod().getParameterTypes();
			for (int i = 0; i < parameterTypes.length; i++) {
				sb.append(parameterTypes[i].getSimpleName());
				if (i < parameterTypes.length - 1)
					sb.append(',');
			}
			sb.append(")");
			method = sb.toString();
		}
		long time = System.currentTimeMillis();
		try {
			Object result = super.invoke(invocation);
			if (serviceStats != null) {
				serviceStats.clientSideEmit(discoveredHost, getServiceInterface().getName(), method.toString(),
						System.currentTimeMillis() - time, false);
			}
			return result;
		} catch (RemoteAccessException e) {
			if (serviceStats != null) {
				serviceStats.clientSideEmit(discoveredHost, getServiceInterface().getName(), method.toString(),
						System.currentTimeMillis() - time, true);
			}
			if (--attempts < 1)
				throw e;
			Throwable throwable = e.getCause();
			if (throwable instanceof SerializationFailedException) {
				setUseFstSerialization(false);
				logger.error("downgrade serialization from fst to java for service[{}]: {}",
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
						logger.info("relocate service url:" + serviceUrl);
					}
				}
			}
			return invoke(invocation, attempts);
		}
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
				logger.error("couldn't discover service:" + serviceName);
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
}
