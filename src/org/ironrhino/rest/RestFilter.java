package org.ironrhino.rest;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.servlet.LoggingBodyHttpServletRequest;
import org.ironrhino.core.servlet.LoggingBodyHttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RestFilter extends OncePerRequestFilter {

	private static final Logger logger = LoggerFactory.getLogger("rest");

	@Value("${restFilter.loggingBody:true}")
	private boolean loggingBody = true;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		boolean skip = !loggingBody;
		if (!skip) {
			for (MediaType accept : MediaType.parseMediaTypes(request.getHeader(HttpHeaders.ACCEPT))) {
				if (accept.equals(MediaType.TEXT_EVENT_STREAM)) {
					skip = true;
					break;
				}
				if (accept.isCompatibleWith(MediaType.APPLICATION_JSON)
						|| accept.isCompatibleWith(MediaType.TEXT_PLAIN)) {
					skip = false;
					break;
				}
			}
		}
		if (skip) {
			filterChain.doFilter(request, response);
			return;
		}

		if (request.getContentType() == null)
			request = new WrappedHttpServletRequest(request);
		String contentType = request.getContentType();
		if (contentType == null || contentType.startsWith(MediaType.TEXT_PLAIN_VALUE)
				|| contentType.startsWith(MediaType.APPLICATION_JSON_VALUE)) {
			if (request.getMethod().equals("GET") || request.getMethod().equals("DELETE")) {
				logger.info("");
			} else {
				request = new LoggingBodyHttpServletRequest(request, logger);
			}
			response = new LoggingBodyHttpServletResponse(response, logger, request.getCharacterEncoding());
		}

		filterChain.doFilter(request, response);
		if (response instanceof LoggingBodyHttpServletResponse) {
			contentType = response.getContentType();
			if (contentType != null && (contentType.startsWith(MediaType.TEXT_PLAIN_VALUE)
					|| contentType.startsWith(MediaType.APPLICATION_JSON_VALUE)))
				response.getOutputStream().close();
		}
	}

	private static class WrappedHttpServletRequest extends HttpServletRequestWrapper {

		public WrappedHttpServletRequest(HttpServletRequest request) {
			super(request);
		}

		@Override
		public String getContentType() {
			String contentType = super.getContentType();
			return StringUtils.isNotBlank(contentType) ? contentType : MediaType.APPLICATION_JSON_VALUE;
		}

		@Override
		public String getHeader(String name) {
			if (StringUtils.equalsIgnoreCase(HttpHeaders.CONTENT_TYPE, name)) {
				return getContentType();
			}
			return super.getHeader(name);
		}

		@Override
		public Enumeration<String> getHeaders(String name) {
			if (StringUtils.equalsIgnoreCase(HttpHeaders.CONTENT_TYPE, name)) {
				return Collections.enumeration(Arrays.asList(getContentType()));
			}
			return super.getHeaders(name);
		}

	}

}
