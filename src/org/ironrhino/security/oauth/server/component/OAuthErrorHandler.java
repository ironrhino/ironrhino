package org.ironrhino.security.oauth.server.component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.JsonUtils;
import org.ironrhino.security.oauth.server.domain.OAuthError;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class OAuthErrorHandler {

	@Value("${oauth.error.legacy:false}")
	private boolean legacy;

	public void handle(HttpServletRequest request, HttpServletResponse response, OAuthError oauthError)
			throws IOException {
		int status = determineHttpStatusCode(oauthError);
		response.setStatus(status);
		response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
		if (!legacy) {
			response.getWriter().write(JsonUtils.toJson(oauthError));
		} else {
			Map<String, String> map = new LinkedHashMap<>();
			map.put("code", status == HttpServletResponse.SC_UNAUTHORIZED ? "3" : "7");
			map.put("status", status == HttpServletResponse.SC_UNAUTHORIZED ? "UNAUTHORIZED" : "BAD_REQUEST");
			String message = oauthError.getError();
			if (StringUtils.isNotBlank(oauthError.getErrorMessage()))
				message = oauthError.getErrorMessage();
			if (message.startsWith("client_"))
				message = message.toUpperCase(Locale.ROOT);
			map.put("message", message);
			response.getWriter().write(JsonUtils.toJson(map));
		}
	}

	protected int determineHttpStatusCode(OAuthError oauthError) {
		String error = oauthError.getError();
		if (error.equals(OAuthError.INVALID_TOKEN) || error.contains("unauthorized"))
			return HttpServletResponse.SC_UNAUTHORIZED;
		if (error.equals(OAuthError.INSUFFICIENT_SCOPE))
			return HttpServletResponse.SC_FORBIDDEN;
		return HttpServletResponse.SC_BAD_REQUEST;
	}

}
