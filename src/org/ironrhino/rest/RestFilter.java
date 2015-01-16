package org.ironrhino.rest;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.RequestUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

public class RestFilter extends OncePerRequestFilter {

	public static final String PARAMETER_NAME_METHOD = "_method";

	@Override
	protected void doFilterInternal(HttpServletRequest request,
			HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		Map<String, String> map = RequestUtils
				.parseParametersFromQueryString(request.getQueryString());
		String method = map.get(PARAMETER_NAME_METHOD);
		if (StringUtils.isNotBlank(method)
				|| request.getContentType() != null
				&& !request.getContentType().startsWith(
						MediaType.APPLICATION_JSON_VALUE))
			request = new WrappedHttpServletRequest(request,
					StringUtils.isNotBlank(method) ? method.toUpperCase()
							.trim() : request.getMethod());
		filterChain.doFilter(request, response);
	}

	private static class WrappedHttpServletRequest extends
			HttpServletRequestWrapper {

		private String method;

		public WrappedHttpServletRequest(HttpServletRequest request,
				String method) {
			super(request);
			this.method = method;
		}

		@Override
		public String getMethod() {
			return method;
		}

		@Override
		public String getContentType() {
			return MediaType.APPLICATION_JSON_VALUE;
		}

		@Override
		public String getHeader(String name) {
			if (StringUtils.equalsIgnoreCase(HttpHeaders.CONTENT_TYPE, name)) {
				return MediaType.APPLICATION_JSON_VALUE;
			}
			return super.getHeader(name);
		}

		@Override
		public Enumeration<String> getHeaders(String name) {
			if (StringUtils.equalsIgnoreCase(HttpHeaders.CONTENT_TYPE, name)) {
				return Collections.enumeration(Arrays
						.asList(MediaType.APPLICATION_JSON_VALUE));
			}
			return super.getHeaders(name);
		}

	}

}
