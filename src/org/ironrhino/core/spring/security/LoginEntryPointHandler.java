package org.ironrhino.core.spring.security;

import javax.servlet.http.HttpServletRequest;

public interface LoginEntryPointHandler {

	String handle(HttpServletRequest request, String targetUrl);

}