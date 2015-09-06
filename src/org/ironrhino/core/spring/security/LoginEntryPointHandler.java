package org.ironrhino.core.spring.security;

import javax.servlet.http.HttpServletRequest;

public interface LoginEntryPointHandler {

	public String handle(HttpServletRequest request, String targetUrl);

}