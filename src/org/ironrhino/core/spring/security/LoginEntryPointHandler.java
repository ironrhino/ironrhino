package org.ironrhino.core.spring.security;

import javax.servlet.http.HttpServletRequest;

@FunctionalInterface
public interface LoginEntryPointHandler {

	String handle(HttpServletRequest request, String targetUrl);

}