package org.ironrhino.core.remoting.client;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.metadata.PostPropertiesReset;
import org.ironrhino.core.remoting.RemotingContext;
import org.ironrhino.core.remoting.ServiceRegistry;
import org.ironrhino.core.servlet.AccessFilter;
import org.ironrhino.core.spring.RemotingClientProxy;
import org.ironrhino.core.util.AppInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.caucho.HessianClientInterceptor;
import org.springframework.util.Assert;

import com.caucho.hessian.client.HessianConnection;
import com.caucho.hessian.client.HessianProxyFactory;
import com.caucho.hessian.client.HessianURLConnectionFactory;

public class HessianClient extends HessianClientInterceptor implements FactoryBean<Object> {

	private static final String SERVLET_PATH_PREFIX = "/remoting/hessian/";

	private static Logger logger = LoggerFactory.getLogger(HessianClient.class);

	@Autowired(required = false)
	private ServiceRegistry serviceRegistry;

	@Value("${remoting.channel.secure:false}")
	private boolean secure;

	private String host;

	private int port;

	private String contextPath;

	private int maxAttempts = 3;

	private boolean urlFromDiscovery;

	private boolean discovered; // for lazy discover from serviceRegistry

	private String discoveredHost;

	private boolean reset;

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

	@Override
	public void setServiceUrl(String serviceUrl) {
		super.setServiceUrl(serviceUrl);
		reset = true;
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
				Map<String, String> map = RemotingContext.getContext();
				if (map != null) {
					for (Map.Entry<String, String> entry : map.entrySet())
						conn.addHeader(RemotingContext.HTTP_HEADER_PREFIX + URLEncoder.encode(entry.getKey(), "UTF-8"),
								URLEncoder.encode(entry.getValue(), "UTF-8"));
				}
				return conn;
			}

		});
		setProxyFactory(proxyFactory);
		super.afterPropertiesSet();
		ProxyFactory pf = new ProxyFactory(getServiceInterface(), this);
		pf.addInterface(RemotingClientProxy.class);
		this.serviceProxy = pf.getProxy(getBeanClassLoader());
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
		try {
			return invoke(invocation, maxAttempts);
		} finally {
			RemotingContext.clear();
		}

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
