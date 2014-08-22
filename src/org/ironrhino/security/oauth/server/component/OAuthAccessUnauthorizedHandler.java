package org.ironrhino.security.oauth.server.component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface OAuthAccessUnauthorizedHandler {

	public void handle(HttpServletRequest request,
			HttpServletResponse response, String message);

}
