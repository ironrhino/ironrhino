package org.ironrhino.core.remoting.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ironrhino.core.remoting.FstHttpInvokerSerializationHelper;
import org.ironrhino.core.remoting.ServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.remoting.httpinvoker.HttpInvokerServiceExporter;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;

public class HttpInvokerServer extends HttpInvokerServiceExporter {

	protected static final String CONTENT_TYPE_FST_SERIALIZED_OBJECT = "application/x-fst-serialized-object";

	protected static final String HTTP_HEADER_CONTENT_TYPE = "Content-Type";

	private Logger logger = LoggerFactory.getLogger(getClass());

	private static ThreadLocal<Class<?>> serviceInterface = new ThreadLocal<>();

	private static ThreadLocal<Object> service = new ThreadLocal<>();

	private Map<Class<?>, Object> proxies = new HashMap<>();

	@Autowired(required = false)
	private ServiceRegistry serviceRegistry;

	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public void handleRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String uri = request.getRequestURI();
		try {
			String interfaceName = uri.substring(uri.lastIndexOf('/') + 1);
			Class<?> clazz = Class.forName(interfaceName);
			serviceInterface.set(clazz);
			RemoteInvocation invocation = readRemoteInvocation(request);
			Object proxy = getProxyForService();
			if (proxy != null) {
				RemoteInvocationResult result = invokeAndCreateResult(invocation, proxy);
				writeRemoteInvocationResult(request, response, result);
			} else {
				String msg = "No Service:" + getServiceInterface().getName();
				logger.error("No Service:" + getServiceInterface());
				response.sendError(HttpServletResponse.SC_NOT_FOUND, msg);
			}
		} catch (SerializationFailedException sfe) {
			RemoteInvocationResult result = new RemoteInvocationResult();
			result.setException(sfe);
			writeRemoteInvocationResult(request, response, result);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
		} finally {
			serviceInterface.remove();
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
		boolean useFstSerialization = CONTENT_TYPE_FST_SERIALIZED_OBJECT
				.equals(request.getHeader(HTTP_HEADER_CONTENT_TYPE));
		if (useFstSerialization) {
			try {
				return FstHttpInvokerSerializationHelper.readRemoteInvocation(is);
			} catch (IOException e) {
				throw new SerializationFailedException(e.getMessage(), e);
			}
		} else {
			return super.readRemoteInvocation(request, is);
		}
	}

	@Override
	protected void writeRemoteInvocationResult(HttpServletRequest request, HttpServletResponse response,
			RemoteInvocationResult result, OutputStream os) throws IOException {
		boolean useFstSerialization = CONTENT_TYPE_FST_SERIALIZED_OBJECT
				.equals(request.getHeader(HTTP_HEADER_CONTENT_TYPE));
		if (useFstSerialization)
			FstHttpInvokerSerializationHelper.writeRemoteInvocationResult(result, os);
		else
			super.writeRemoteInvocationResult(request, response, result, os);
	}

}
