package org.ironrhino.core.remoting.client;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.remoting.ServiceRegistry;
import org.ironrhino.core.util.AppInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.httpinvoker.HttpInvokerProxyFactoryBean;
import org.springframework.remoting.httpinvoker.HttpInvokerRequestExecutor;
import org.springframework.util.Assert;

public class HttpInvokerClient extends HttpInvokerProxyFactoryBean {

	private static Logger logger = LoggerFactory.getLogger(HttpInvokerClient.class);

	private HttpInvokerRequestExecutor httpInvokerRequestExecutor;

	@Autowired(required = false)
	private ServiceRegistry serviceRegistry;

	@Autowired(required = false)
	private ExecutorService executorService;

	private String host;

	private int port;

	private String contextPath;

	private int maxAttempts = 3;

	private List<String> asyncMethods;

	private boolean poll;

	private boolean urlFromDiscovery;

	private boolean discovered; // for lazy discover from serviceRegistry

	private String discoveredHost;

	public void setPoll(boolean poll) {
		this.poll = poll;
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

	public void setAsyncMethods(String asyncMethods) {
		if (StringUtils.isNotBlank(asyncMethods)) {
			asyncMethods = asyncMethods.trim();
			String[] array = asyncMethods.split("\\s*,\\s*");
			this.asyncMethods = Arrays.asList(array);
		}
	}

	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	public void setExecutorService(ExecutorService executorService) {
		this.executorService = executorService;
	}

	@Override
	public HttpInvokerRequestExecutor getHttpInvokerRequestExecutor() {
		if (this.httpInvokerRequestExecutor == null) {
			SimpleHttpInvokerRequestExecutor executor = new SimpleHttpInvokerRequestExecutor();
			executor.setBeanClassLoader(getBeanClassLoader());
			this.httpInvokerRequestExecutor = executor;
		}
		return this.httpInvokerRequestExecutor;
	}

	@Override
	public void setHttpInvokerRequestExecutor(HttpInvokerRequestExecutor httpInvokerRequestExecutor) {
		this.httpInvokerRequestExecutor = httpInvokerRequestExecutor;
	}

	@Override
	public void afterPropertiesSet() {
		if (port <= 0)
			port = AppInfo.getHttpPort();
		if (port <= 0)
			port = 8080;
		String serviceUrl = getServiceUrl();
		if (serviceUrl == null) {
			Assert.notNull(serviceRegistry);
			setServiceUrl("http://fakehost/");
			discovered = false;
			urlFromDiscovery = true;
		}
		super.afterPropertiesSet();
	}

	@Override
	public Object invoke(final MethodInvocation invocation) throws Throwable {
		if (!discovered) {
			setServiceUrl(discoverServiceUrl());
			discovered = true;
		} else if (poll) {
			setServiceUrl(discoverServiceUrl());
		}
		if (asyncMethods != null) {
			String name = invocation.getMethod().getName();
			if (asyncMethods.contains(name)) {
				Runnable task = () -> {
					try {
						invoke(invocation, maxAttempts);
					} catch (Throwable e) {
						logger.error(e.getMessage(), e);
					}
				};
				if (executorService != null)
					executorService.execute(task);
				else
					new Thread(task).start();
				return null;
			}
		}
		return invoke(invocation, maxAttempts);
	}

	public Object invoke(MethodInvocation invocation, int attempts) throws Throwable {
		try {
			return super.invoke(invocation);
		} catch (RemoteAccessException e) {
			if (--attempts < 1)
				throw e;
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
		StringBuilder sb = new StringBuilder("http://");
		if (StringUtils.isBlank(host)) {
			if (serviceRegistry != null) {
				String ho = serviceRegistry.discover(serviceName);
				if (ho != null) {
					sb.append(ho);
					if (ho.indexOf(':') < 0 && port != 80) {
						sb.append(':');
						sb.append(port);
					}
					discoveredHost = ho;
				} else {
					sb.append("fakehost");
					logger.error("couldn't discover service:" + serviceName);
				}
			} else {
				sb.append("fakehost");
			}

		} else {
			sb.append(host);
			if (port != 80) {
				sb.append(':');
				sb.append(port);
			}
		}

		if (StringUtils.isNotBlank(contextPath))
			sb.append(contextPath);
		sb.append("/remoting/httpinvoker/");
		sb.append(serviceName);
		return sb.toString();
	}
}
