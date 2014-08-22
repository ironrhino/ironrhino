package org.ironrhino.core.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AccessHandler {

	public String getPattern() {
		return null;
	}

	public String getExcludePattern() {
		return null;
	}

	public boolean handle(HttpServletRequest request,
			HttpServletResponse response) {
		return false;
	}

}
