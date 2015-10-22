package org.ironrhino.core.remoting.client;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.metadata.PostPropertiesReset;
import org.ironrhino.core.remoting.ServiceRegistry;
import org.ironrhino.core.servlet.AccessFilter;
import org.ironrhino.core.util.AppInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.caucho.HessianProxyFactoryBean;
import org.springframework.util.Assert;

import com.caucho.hessian.client.HessianConnection;
import com.caucho.hessian.client.HessianProxyFactory;
import com.caucho.hessian.client.HessianURLConnectionFactory;

public class HessianClient extends HessianProxyFactoryBean {

	private static Logger logger = LoggerFactory.getLogger(HessianClient.class);

	private ServiceRegistry serviceRegistry;

	private ExecutorService executorService;

	private String host;

	private int port;

	private String contextPath;

	private int maxAttempts = 3;

	private List<String> asyncMethods;

	private boolean urlFromDiscovery;

	private boolean discovered; // for lazy discover from serviceRegistry

	private boolean reset;

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
	public void setServiceUrl(String serviceUrl) {
		super.setServiceUrl(serviceUrl);
		reset = true;
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
			reset = false;
			discovered = false;
			urlFromDiscovery = true;
		}
		setHessian2(true);
		final HessianProxyFactory proxyFactory = new HessianProxyFactory();
		proxyFactory.setConnectionFactory(new HessianURLConnectionFactory() {

			{
				setHessianProxyFactory(proxyFactory);
			}

			@Override
			public HessianConnection open(URL url) throws IOException {
				HessianConnection conn = super.open(url);
				String requestId = MDC.get(AccessFilter.MDC_KEY_REQUEST_ID);
				if (requestId != null)
					conn.addHeader(AccessFilter.HTTP_HEADER_REQUEST_ID, requestId);
				return conn;
			}

		});
		setProxyFactory(proxyFactory);
		super.afterPropertiesSet();
	}

	@PostPropertiesReset
	public void reset() {
		if (reset) {
			reset = false;
			super.afterPropertiesSet();
		}
	}

	@Override
	public Object invoke(final MethodInvocation invocation) throws Throwable {
		if (urlFromDiscovery && !discovered) {
			String serviceUrl = discoverServiceUrl();
			setServiceUrl(serviceUrl);
			reset();
			discovered = true;
		}
		if (asyncMethods != null) {
			String name = invocation.getMethod().getName();
			if (asyncMethods.contains(name)) {
				Runnable task = new Runnable() {
					@Override
					public void run() {
						try {
							invoke(invocation, maxAttempts);
						} catch (Throwable e) {
							logger.error(e.getMessage(), e);
						}
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
				serviceRegistry.evict(host);
				String serviceUrl = discoverServiceUrl();
				if (!serviceUrl.equals(getServiceUrl())) {
					setServiceUrl(serviceUrl);
					logger.info("relocate service url:" + serviceUrl);
					reset();
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
		sb.append("/remoting/hessian/");
		sb.append(serviceName);
		return sb.toString();
	}
}
