package org.ironrhino.rest.component;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ironrhino.core.servlet.HttpErrorHandler;
import org.ironrhino.core.util.JsonUtils;
import org.ironrhino.core.util.RequestUtils;
import org.ironrhino.rest.RestStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class HttpErrorHandlerImpl implements HttpErrorHandler {

	@Override
	public boolean handle(HttpServletRequest request, HttpServletResponse response, int statusCode, String message) {
		String requestURI = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
		if (requestURI == null)
			requestURI = RequestUtils.getRequestUri(request);
		if (!(requestURI.startsWith("/api/") || requestURI.startsWith("/oauth/oauth2/")))
			return false;
		if (statusCode > 0)
			response.setStatus(statusCode);
		RestStatus rs = null;
		switch (statusCode) {
		case HttpServletResponse.SC_OK:
			rs = RestStatus.OK;
			break;
		case HttpServletResponse.SC_UNAUTHORIZED:
			rs = RestStatus.valueOf(RestStatus.CODE_UNAUTHORIZED, message);
			break;
		case HttpServletResponse.SC_NOT_FOUND:
			rs = RestStatus.NOT_FOUND;
			break;
		case HttpServletResponse.SC_BAD_REQUEST:
			rs = RestStatus.valueOf(RestStatus.CODE_BAD_REQUEST, message);
			break;
		default:
			rs = RestStatus.valueOf(RestStatus.CODE_FORBIDDEN, message);
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
