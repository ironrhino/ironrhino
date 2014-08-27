package org.ironrhino.api;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ironrhino.core.servlet.HttpErrorHandler;
import org.ironrhino.core.util.JsonUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class HttpErrorHandlerImpl implements HttpErrorHandler {

	@Override
	public boolean handle(HttpServletRequest request,
			HttpServletResponse response, int statusCode, String message) {
		String requestURI = (String) request
				.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
		if (requestURI == null)
			requestURI = request.getRequestURI();
		if (!requestURI.startsWith("/api/"))
			return false;
		RestStatus rs = null;
		switch (statusCode) {
		case HttpServletResponse.SC_UNAUTHORIZED:
			rs = RestStatus.valueOf(RestStatus.CODE_UNAUTHORIZED, message);
			break;
		case HttpServletResponse.SC_NOT_FOUND:
			rs = RestStatus.NOT_FOUND;
			break;

		default:
			rs = RestStatus.FORBIDDEN;
			break;
		}
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		try {
			response.getWriter().write(JsonUtils.toJson(rs));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

}
