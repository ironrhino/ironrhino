package org.ironrhino.core.remoting.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Method;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ironrhino.core.remoting.ServiceRegistry;
import org.ironrhino.core.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.HttpRequestHandler;

import com.fasterxml.jackson.databind.JsonNode;

public class JsonCallServer implements HttpRequestHandler {

	private Logger log = LoggerFactory.getLogger(getClass());

	private ServiceRegistry serviceRegistry;

	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public void handleRequest(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		String httpMethod = request.getMethod();
		if (!"POST".equalsIgnoreCase(httpMethod)
				&& !"PUT".equalsIgnoreCase(httpMethod)) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST,
					"Only Accept Post or Put Request");
			return;
		}
		String uri = request.getRequestURI();
		try {
			String[] arr = uri.split("/");
			String methodName = arr[arr.length - 1];
			String interfaceName = arr[arr.length - 2];
			Object bean = serviceRegistry.getExportServices()
					.get(interfaceName);
			if (bean == null) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST,
						"No Such Service");
				return;
			}
			String requestBody = null;
			JsonNode requestJsonParameters = null;
			BufferedReader reader = request.getReader();
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null)
				sb.append(line).append("\n");
			reader.close();
			if (sb.length() > 0) {
				sb.deleteCharAt(sb.length() - 1);
				requestBody = sb.toString();
				if (!JsonUtils.isValidJson(requestBody)) {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST,
							"Invalid JSON");
					return;
				}
				requestJsonParameters = JsonUtils.createNewObjectMapper()
						.readValue(requestBody, JsonNode.class);
				if (!requestJsonParameters.isArray()) {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST,
							"JSON Must Be Array");
					return;
				}
			}

			Class<?> clazz = Class.forName(interfaceName);
			Object[] parameters = null;
			Method method = null;
			Method[] methods = clazz.getMethods();
			loop: for (Method m : methods) {
				if (m.getName().equals(methodName)) {
					Class<?>[] parameterTypes = m.getParameterTypes();
					if (requestJsonParameters == null
							&& parameterTypes.length == 0) {
						method = m;
						parameters = new Object[0];
						break;
					}
					if (requestJsonParameters == null
							&& parameterTypes.length > 0
							|| requestJsonParameters != null
							&& requestJsonParameters.size() != parameterTypes.length)
						continue;

					parameters = new Object[parameterTypes.length];
					for (int i = 0; i < parameterTypes.length; i++) {

						if (requestJsonParameters == null
								|| requestJsonParameters.get(i) == null)
							continue;
						JsonNode node = requestJsonParameters.get(i);
						try {
							parameters[i] = JsonUtils.fromJson(node.toString(),
									parameterTypes[i]);
						} catch (Exception e) {
							if (node.isContainerNode()) {
								continue loop;
							} else {
								if (node.isBoolean())
									parameters[i] = node.asBoolean();
								else if (node.isLong())
									parameters[i] = node.asLong();
								else if (node.isIntegralNumber())
									parameters[i] = node.asLong();
								else if (node.isFloatingPointNumber())
									parameters[i] = node.asDouble();
								else if (node.isTextual())
									parameters[i] = node.asText();
								else
									continue loop;
							}
						}
					}
					method = m;
					break;
				}
			}

			if (method == null) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST,
						"No Such Method");
				return;
			}
			response.setContentType("application/json;charset=utf-8");
			Object result = method.invoke(bean, parameters);
			if (!method.getReturnType().equals(Void.class)) {
				response.getWriter().write(JsonUtils.toJson(result));
			}
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					ex.getMessage());
		} finally {
		}
	}

}
