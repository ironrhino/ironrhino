package org.ironrhino.core.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RequestContext {

	private static ThreadLocal<HttpServletRequest> requestHolder = new ThreadLocal<>();

	private static ThreadLocal<HttpServletResponse> responseHolder = new ThreadLocal<>();

	public static HttpServletRequest getRequest() {
		return requestHolder.get();
	}

	public static HttpServletResponse getResponse() {
		return responseHolder.get();
	}

	static void set(HttpServletRequest request, HttpServletResponse response) {
		requestHolder.set(request);
		responseHolder.set(response);
	}

	static void reset() {
		requestHolder.remove();
		responseHolder.remove();
	}

}
