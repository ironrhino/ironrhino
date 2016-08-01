package org.ironrhino.core.remoting.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ironrhino.core.remoting.RemotingContext;
import org.ironrhino.core.remoting.SerializationType;
import org.ironrhino.core.remoting.ServiceRegistry;
import org.ironrhino.core.remoting.ServiceStats;
import org.ironrhino.core.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.remoting.httpinvoker.HttpInvokerServiceExporter;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;

public class HttpInvokerServer extends HttpInvokerServiceExporter {

	protected static final String HTTP_HEADER_CONTENT_TYPE = "Content-Type";

	private Logger logger = LoggerFactory.getLogger(getClass());

	private static ThreadLocal<Class<?>> serviceInterface = new ThreadLocal<>();

	private static ThreadLocal<Object> service = new ThreadLocal<>();

	private Map<Class<?>, Object> proxies = new HashMap<>();

	@Value("${httpInvoker.loggingPayload:false}")
	private boolean loggingPayload;

	@Autowired(required = false)
	private ServiceRegistry serviceRegistry;

	@Autowired(required = false)
	private ServiceStats serviceStats;

	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	public void setServiceStats(ServiceStats serviceStat) {
		this.serviceStats = serviceStat;
	}

	@Override
	public void handleRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		Enumeration<String> en = request.getHeaderNames();
		while (en.hasMoreElements()) {
			String name = en.nextElement();
			if (name.startsWith(RemotingContext.HTTP_HEADER_PREFIX)) {
				String key = URLDecoder.decode(name.substring(RemotingContext.HTTP_HEADER_PREFIX.length()), "UTF-8");
				String value = URLDecoder.decode(request.getHeader(name), "UTF-8");
				RemotingContext.put(key, value);
			}
		}
		String uri = request.getRequestURI();
		try {
			String interfaceName = uri.substring(uri.lastIndexOf('/') + 1);
			Class<?> clazz = Class.forName(interfaceName);
			serviceInterface.set(clazz);
			RemoteInvocation invocation = readRemoteInvocation(request);
			Object proxy = getProxyForService();
			if (proxy != null) {
				if (loggingPayload) {
					logger.info("invoking {}.{}() with:\n{}", clazz.getName(), invocation.getMethodName(),
							JsonUtils.toJson(invocation.getArguments()));
				}
				long time = System.currentTimeMillis();
				RemoteInvocationResult result = invokeAndCreateResult(invocation, proxy);
				time = System.currentTimeMillis() - time;
				writeRemoteInvocationResult(request, response, invocation, result);
				if (serviceStats != null) {
					StringBuilder method = new StringBuilder(invocation.getMethodName()).append("(");
					Class<?>[] parameterTypes = invocation.getParameterTypes();
					for (int i = 0; i < parameterTypes.length; i++) {
						method.append(parameterTypes[i].getSimpleName());
						if (i < parameterTypes.length - 1)
							method.append(',');
					}
					method.append(")");
					serviceStats.serverSideEmit(interfaceName, method.toString(), time);
				}
				if (loggingPayload) {
					MDC.remove("url");
					if (!result.hasException()) {
						Object value = result.getValue();
						if (value != null) {
							logger.info("returned in {}ms:\n{}", time, JsonUtils.toJson(value));
						} else {
							logger.info("returned in {}ms: null", time);
						}
					} else {
						Throwable throwable = result.getException();
						if (throwable.getCause() != null)
							throwable = throwable.getCause();
						logger.error(throwable.getMessage(), throwable);
					}
				}
			} else {
				String msg = "No Service:" + getServiceInterface().getName();
				logger.error("No Service:" + getServiceInterface());
				response.sendError(HttpServletResponse.SC_NOT_FOUND, msg);
			}
		} catch (SerializationFailedException sfe) {
			logger.error(sfe.getMessage(), sfe);
			RemoteInvocationResult result = new RemoteInvocationResult();
			result.setException(sfe);
			writeRemoteInvocationResult(request, response, result);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
		} finally {
			serviceInterface.remove();
			RemotingContext.clear();
		}
	}

	@Override
	public void prepare() {
		if (serviceRegistry != null) {
			for (Map.Entry<String, Object> entry : serviceRegistry.getExportServices().entrySet()) {
				try {
					Class<?> intf = Class.forName(entry.getKey());
					serviceInterface.set(intf);
					service.set(entry.getValue());
					proxies.put(intf, super.getProxyForService());
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
			serviceInterface.remove();
			service.remove();
		}
	}

	@Override
	public Object getService() {
		return service.get();
	}

	@Override
	public Class<?> getServiceInterface() {
		return serviceInterface.get();
	}

	@Override
	protected Object getProxyForService() {
		return proxies.get(getServiceInterface());
	}

	@Override
	protected RemoteInvocation readRemoteInvocation(HttpServletRequest request, InputStream is)
			throws IOException, ClassNotFoundException {
		SerializationType serializationType = SerializationType.parse(request.getHeader(HTTP_HEADER_CONTENT_TYPE));
		if (serializationType != SerializationType.JAVA)
			return serializationType.readRemoteInvocation(decorateInputStream(request, is));
		else
			return super.readRemoteInvocation(request, is);
	}

	protected void writeRemoteInvocationResult(HttpServletRequest request, HttpServletResponse response,
			RemoteInvocation invocation, RemoteInvocationResult result) throws IOException {
		SerializationType serializationType = SerializationType.parse(request.getHeader(HTTP_HEADER_CONTENT_TYPE));
		if (serializationType != SerializationType.JAVA) {
			response.setContentType(serializationType.getContentType());
			serializationType.writeRemoteInvocationResult(invocation, result,
					decorateOutputStream(request, response, response.getOutputStream()));
		} else {
			super.writeRemoteInvocationResult(request, response, result);
		}
	}

}
