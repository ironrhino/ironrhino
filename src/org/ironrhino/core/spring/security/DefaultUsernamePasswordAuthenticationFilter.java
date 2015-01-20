package org.ironrhino.core.spring.security;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;

public class DefaultUsernamePasswordAuthenticationFilter extends
		UsernamePasswordAuthenticationFilter {

	@Autowired
	protected AuthenticationFailureHandler authenticationFailureHandler;

	@Autowired
	protected AuthenticationSuccessHandler authenticationSuccessHandler;

	public static final String TARGET_URL = "targetUrl";

	private String filterProcessesUrl;

	public String getFilterProcessesUrl() {
		return filterProcessesUrl;
	}

	public void setFilterProcessesUrl(String filterProcessesUrl) {
		this.filterProcessesUrl = filterProcessesUrl;
	}

	@PostConstruct
	public void init() {
		setAuthenticationFailureHandler(authenticationFailureHandler);
		setAuthenticationSuccessHandler(authenticationSuccessHandler);
		setRequiresAuthenticationRequestMatcher(new FilterProcessUrlRequestMatcher(
				getFilterProcessesUrl()));
	}

	public void success(HttpServletRequest request,
			HttpServletResponse response, Authentication authResult)
			throws IOException, ServletException {
		super.successfulAuthentication(request, response, null, authResult);
	}

	public void unsuccess(HttpServletRequest request,
			HttpServletResponse response, AuthenticationException failed)
			throws IOException, ServletException {
		super.unsuccessfulAuthentication(request, response, failed);
	}

	// replace "endsWith" with "equals"
	private static final class FilterProcessUrlRequestMatcher implements
			RequestMatcher {
		private final String filterProcessesUrl;

		private FilterProcessUrlRequestMatcher(String filterProcessesUrl) {
			Assert.hasLength(filterProcessesUrl,
					"filterProcessesUrl must be specified");
			Assert.isTrue(UrlUtils.isValidRedirectUrl(filterProcessesUrl),
					filterProcessesUrl + " isn't a valid redirect URL");
			this.filterProcessesUrl = filterProcessesUrl;
		}

		public boolean matches(HttpServletRequest request) {
			String uri = request.getRequestURI();
			int pathParamIndex = uri.indexOf(';');

			if (pathParamIndex > 0) {
				// strip everything after the first semi-colon
				uri = uri.substring(0, pathParamIndex);
			}

			if ("".equals(request.getContextPath())) {
				return uri.equals(filterProcessesUrl);
			}

			return uri.equals(request.getContextPath() + filterProcessesUrl);
		}
	}

}
