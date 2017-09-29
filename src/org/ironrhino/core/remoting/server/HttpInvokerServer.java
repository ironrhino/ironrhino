package org.ironrhino.core.remoting.server;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;

import org.ironrhino.core.remoting.RemotingContext;
import org.ironrhino.core.remoting.SerializationType;
import org.ironrhino.core.remoting.ServiceRegistry;
import org.ironrhino.core.remoting.ServiceStats;
import org.ironrhino.core.servlet.AccessFilter;
import org.ironrhino.core.servlet.ProxySupportHttpServletRequest;
import org.ironrhino.core.util.ExceptionUtils;
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

	private Map<String, Object> exportedServices = Collections.emptyMap();

	private Map<String, Class<?>> interfaces = Collections.emptyMap();

	private Map<Class<?>, Object> proxies = Collections.emptyMap();

	@Value("${httpInvoker.loggingPayload:true}")
	private boolean loggingPayload;

	@Autowired
	private ServiceRegistry serviceRegistry;

	@Autowired(required = false)
	private ServiceStats serviceStats;

	@Override
	public void handleRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		if (request instanceof ProxySupportHttpServletRequest) {
			logger.error("Forbidden for Proxy");
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return;
		}
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
		String interfaceName = uri.substring(uri.lastIndexOf('/') + 1);
		Class<?> clazz = interfaces.get(interfaceName);
		if (clazz == null) {
			logger.error("Service Not Found: " + interfaceName);
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		serviceInterface.set(clazz);
		service.set(exportedServices.get(interfaceName));
		Object proxy = getProxyForService();
		try {
			RemoteInvocation invocation = readRemoteInvocation(request);
			List<String> parameterTypeList = new ArrayList<>(invocation.getParameterTypes().length);
			for (Class<?> cl : invocation.getParameterTypes())
				parameterTypeList.add(cl.getSimpleName());
			String method = new StringBuilder(invocation.getMethodName()).append("(")
					.append(String.join(",", parameterTypeList)).append(")").toString();
			MDC.put("role", "SERVER");
			MDC.put("service", interfaceName + '.' + method);
			if (loggingPayload) {
				Object payload;
				Object[] arguments = invocation.getArguments();
				String[] parameterNames = ReflectionUtils
						.getParameterNames(clazz.getMethod(invocation.getMethodName(), invocation.getParameterTypes()));
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
		} catch (SerializationFailedException sfe) {
			logger.error(sfe.getMessage(), sfe);
			RemoteInvocationResult result = new RemoteInvocationResult();
			result.setException(sfe);
			writeRemoteInvocationResult(request, response, result);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		} finally {
			serviceInterface.remove();
			service.remove();
			RemotingContext.clear();
			MDC.remove("role");
			MDC.remove("service");
		}
	}

	@Override
	public void prepare() {
		if (serviceRegistry != null) {
			exportedServices = serviceRegistry.getExportedServices();
			proxies = new HashMap<>(exportedServices.size() * 3 / 2 + 1, 1f);
			interfaces = new HashMap<>(exportedServices.size() * 3 / 2 + 1, 1f);
			for (Map.Entry<String, Object> entry : exportedServices.entrySet()) {
				try {
					Class<?> intf = Class.forName(entry.getKey());
					serviceInterface.set(intf);
					service.set(entry.getValue());
					proxies.put(intf, super.getProxyForService());
					interfaces.put(entry.getKey(), intf);
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
				InvocationTargetException ite = (InvocationTargetException) result.getException();
				ReflectionUtils.setFieldValue(ite, "target", translateAndTrim(ite.getTargetException(), 10));
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

	protected Throwable translateAndTrim(Throwable throwable, int maxStackTraceElements) {
		if (throwable instanceof ConstraintViolationException) {
			ValidationException ve = new ValidationException(throwable.getMessage());
			ve.setStackTrace(throwable.getStackTrace());
			throwable = ve;
		}
		ExceptionUtils.trimStackTrace(throwable, maxStackTraceElements);
		return throwable;
	}

}
