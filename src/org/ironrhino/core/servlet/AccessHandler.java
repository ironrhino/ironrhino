package org.ironrhino.core.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface AccessHandler {

	public String getPattern();

	public boolean handle(HttpServletRequest request,
			HttpServletResponse response);

}
