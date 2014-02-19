package org.ironrhino.core.remoting.client;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.StringEntity;
import org.ironrhino.core.remoting.ServiceRegistry;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.HttpClientUtils;
import org.ironrhino.core.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.remoting.support.RemoteInvocationBasedAccessor;
import org.springframework.util.Assert;

public class JsonCallClient extends RemoteInvocationBasedAccessor implements
		MethodInterceptor, FactoryBean<Object> {

	private static Logger log = LoggerFactory.getLogger(JsonCallClient.class);

	private ServiceRegistry serviceRegistry;

	private ExecutorService executorService;

	private String host;

	private int port;

	private String contextPath;

	private int maxRetryTimes = 3;

	private List<String> asyncMethods;

	private boolean urlFromDiscovery;

	private boolean discovered; // for lazy discover from serviceRegistry

	private boolean poll;

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

	public void setMaxRetryTimes(int maxRetryTimes) {
		this.maxRetryTimes = maxRetryTimes;
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
		if (getServiceInterface() == null) {
			throw new IllegalArgumentException(
					"Property 'serviceInterface' is required");
		}
		this.serviceProxy = new ProxyFactory(getServiceInterface(), this)
				.getProxy(getBeanClassLoader());
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
				Runnable task = new Runnable() {
					@Override
					public void run() {
						try {
							invoke(invocation, maxRetryTimes);
						} catch (Throwable e) {
							log.error(e.getMessage(), e);
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
		return invoke(invocation, maxRetryTimes);
	}

	public Object invoke(MethodInvocation invocation, int retryTimes)
			throws Throwable {
		retryTimes--;
		try {
			String url = getServiceUrl() + "/"
					+ invocation.getMethod().getName();
			HttpPost postMethod = new HttpPost(url);
			if (invocation.getMethod().getParameterTypes().length > 0)
				postMethod.setEntity(new StringEntity(JsonUtils
						.toJson(invocation.getArguments())));
			HttpResponse rsp = HttpClientUtils.getDefaultInstance().execute(
					postMethod);
			StatusLine sl = rsp.getStatusLine();
			if (sl.getStatusCode() >= 300) {
				throw new RuntimeException(
						"Did not receive successful HTTP response: status code = "
								+ sl.getStatusCode() + ", status message = ["
								+ sl.getReasonPhrase() + "]");
			}
			HttpEntity entity = rsp.getEntity();
			StringBuilder sb = new StringBuilder();
			InputStream is = entity.getContent();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					is, "utf-8"));
			String line;
			while ((line = reader.readLine()) != null)
				sb.append(line).append("\n");
			reader.close();
			is.close();
			String responseBody = null;
			if (sb.length() > 0) {
				sb.deleteCharAt(sb.length() - 1);
				responseBody = sb.toString();
			}
			Type t = invocation.getMethod().getGenericReturnType();
			if (t.equals(Void.class) || responseBody == null)
				return null;
			return JsonUtils.fromJson(responseBody, t);
		} catch (ConnectTimeoutException e) {
			if (retryTimes < 0)
				throw e;
			if (urlFromDiscovery) {
				serviceRegistry.evict(host);
				String serviceUrl = discoverServiceUrl();
				if (!serviceUrl.equals(getServiceUrl())) {
					setServiceUrl(serviceUrl);
					log.info("relocate service url:" + serviceUrl);
				}
			}
			return invoke(invocation, retryTimes);
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
					log.error("couldn't discover service:" + serviceName);
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
		sb.append("/remoting/jsoncall/");
		sb.append(serviceName);
		return sb.toString();
	}

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

}
