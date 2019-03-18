package org.ironrhino.core.servlet;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class ProxySupportHttpServletResponse extends HttpServletResponseWrapper {

	private final HttpServletRequest request;

	public ProxySupportHttpServletResponse(HttpServletRequest request, HttpServletResponse response) {
		super(response);
		this.request = request;
	}

	@Override
	public void sendRedirect(String location) throws IOException {
		if (location.indexOf("://") < 0 && !location.startsWith("//")) {
			String url = request.getRequestURL().toString();
			if (location.startsWith("/")) {
				location = url.substring(0, url.indexOf('/', url.indexOf("://") + 3)) + location;
			} else {
				location = url.substring(0, url.lastIndexOf('/')) + location;
			}
		}
		super.sendRedirect(location);
	}

}
