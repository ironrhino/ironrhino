package org.ironrhino.core.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface HttpErrorHandler {

	public boolean handle(HttpServletRequest request, HttpServletResponse response, int statusCode, String message);

}
