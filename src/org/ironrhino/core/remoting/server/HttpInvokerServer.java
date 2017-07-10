package org.ironrhino.core.remoting.server;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.remoting.RemotingContext;
import org.ironrhino.core.remoting.SerializationType;
import org.ironrhino.core.remoting.ServiceRegistry;
import org.ironrhino.core.remoting.ServiceStats;
import org.ironrhino.core.servlet.AccessFilter;
import org.ironrhino.core.util.JsonDesensitizer;
import org.ironrhino.core.util.ReflectionUtils;
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

	private Logger remotingLogger = LoggerFactory.getLogger("remoting");

	private static ThreadLocal<Class<?>> serviceInterface = new ThreadLocal<>();

	private static ThreadLocal<Object> service = new ThreadLocal<>();

	private Map<Class<?>, Object> proxies = new HashMap<>();

	@Value("${httpInvoker.loggingPayload:true}")
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
		RemotingContext.setRequestFrom(request.getHeader(AccessFilter.HTTP_HEADER_REQUEST_FROM));
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
				List<String> parameterTypeList = new ArrayList<>(invocation.getParameterTypes().length);
				for (Class<?> cl : invocation.getParameterTypes())
					parameterTypeList.add(cl.getSimpleName());
				String method = new StringBuilder(invocation.getMethodName()).append("(")
						.append(StringUtils.join(parameterTypeList, ",")).append(")").toString();
				MDC.put("role", "SERVER");
				MDC.put("service", interfaceName + '.' + method);
				if (loggingPayload)
					remotingLogger.info("Request: {}",
							JsonDesensitizer.DEFAULT_INSTANCE.toJson(invocation.getArguments()));
				long time = System.currentTimeMillis();
				RemoteInvocationResult result = invokeAndCreateResult(invocation, proxy);
				time = System.currentTimeMillis() - time;
				writeRemoteInvocationResult(request, response, invocation, result);
				if (serviceStats != null) {
					serviceStats.serverSideEmit(interfaceName, method, time);
				}
				if (loggingPayload) {
					if (!result.hasException()) {
						Object value = result.getValue();
						remotingLogger.info("Response: {}", JsonDesensitizer.DEFAULT_INSTANCE.toJson(value));
					} else {
						Throwable throwable = result.getException();
						if (throwable.getCause() != null)
							throwable = throwable.getCause();
						remotingLogger.error("Error:", throwable);
					}
				}
				remotingLogger.info("Invoked from {} in {}ms", RemotingContext.getRequestFrom(), time);
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
			MDC.remove("role");
			MDC.remove("service");
		}
	}

	@Override
	public void prepare() {
		if (serviceRegistry != null) {
			for (Map.Entry<String, Object> entry : serviceRegistry.getExportedServices().entrySet()) {
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
		if (result.hasInvocationTargetException()) {
			try {
				trimStackTraceElements(((InvocationTargetException) result.getException()).getTargetException(), 5);
			} catch (Exception ex) {
			}
		}
		SerializationType serializationType = SerializationType.parse(request.getHeader(HTTP_HEADER_CONTENT_TYPE));
		if (serializationType != SerializationType.JAVA) {
			response.setContentType(serializationType.getContentType());
			serializationType.writeRemoteInvocationResult(invocation, result,
					decorateOutputStream(request, response, response.getOutputStream()));
		} else {
			super.writeRemoteInvocationResult(request, response, result);
		}
	}

	private static void trimStackTraceElements(Throwable throwable, int maxStackTraceElements) {
		StackTraceElement[] elements = ReflectionUtils.getFieldValue(throwable, "stackTrace");
		if (elements.length > maxStackTraceElements) {
			System.out.println(elements.length);
			ReflectionUtils.setFieldValue(throwable, "stackTrace",
					Arrays.copyOfRange(elements, 0, maxStackTraceElements));
		}
	}
}
