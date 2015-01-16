package org.ironrhino.rest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.RequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RestFilter extends OncePerRequestFilter {

	public static final String PARAMETER_NAME_METHOD = "_method";

	private static final Logger logger = LoggerFactory.getLogger("rest");

	@Value("${restFilter.loggingBody:true}")
	private boolean loggingBody = true;

	@Override
	protected void doFilterInternal(HttpServletRequest request,
			HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		Map<String, String> map = RequestUtils
				.parseParametersFromQueryString(request.getQueryString());
		String method = map.get(PARAMETER_NAME_METHOD);
		if (StringUtils.isNotBlank(method)
				|| request.getContentType() != null
				&& request.getContentType().startsWith(
						MediaType.TEXT_PLAIN_VALUE))
			request = new WrappedHttpServletRequest(request,
					StringUtils.isNotBlank(method) ? method.toUpperCase()
							.trim() : request.getMethod());
		if (loggingBody
				&& (request.getContentType() == null || request
						.getContentType().startsWith(
								MediaType.APPLICATION_JSON_VALUE))) {
			if (request.getMethod().equalsIgnoreCase("GET")) {
				logger.info("");
			} else {
				request = new LoggingBodyHttpServletRequest(request);
			}
			response = new LoggingBodyHttpServletResponse(response);
		}
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

	private static class LoggingBodyHttpServletRequest extends
			HttpServletRequestWrapper {

		private volatile ServletInputStream servletInputStream;

		private ByteArrayOutputStream baos = new ByteArrayOutputStream(2048);

		public LoggingBodyHttpServletRequest(HttpServletRequest request) {
			super(request);
		}

		@Override
		public ServletInputStream getInputStream() throws IOException {
			final ServletInputStream is = super.getInputStream();
			if (servletInputStream == null) {
				synchronized (this) {
					if (servletInputStream == null) {
						servletInputStream = new ServletInputStream() {

							@Override
							public int read() throws IOException {
								int i = is.read();
								if (i != -1)
									baos.write(i);
								return i;
							}

							@Override
							public void setReadListener(
									ReadListener readListener) {
								is.setReadListener(readListener);

							}

							@Override
							public boolean isReady() {
								return is.isReady();
							}

							@Override
							public boolean isFinished() {
								return is.isFinished();
							}

							@Override
							public void close() throws IOException {
								super.close();
								byte[] bytes = baos.toByteArray();
								baos.close();
								baos = null;
								String encoding = getCharacterEncoding();
								if (encoding == null)
									encoding = "UTF-8";
								logger.info("\n{}", new String(bytes, 0,
										bytes.length, encoding));
							}

						};
					}
				}
			}
			return servletInputStream;
		}

	}

	private static class LoggingBodyHttpServletResponse extends
			HttpServletResponseWrapper {

		private volatile ServletOutputStream streamOutputStream;

		private ByteArrayOutputStream baos = new ByteArrayOutputStream(2048);

		public LoggingBodyHttpServletResponse(HttpServletResponse response) {
			super(response);
		}

		@Override
		public ServletOutputStream getOutputStream() throws IOException {

			final ServletOutputStream os = super.getOutputStream();
			if (streamOutputStream == null) {
				synchronized (this) {
					if (streamOutputStream == null) {
						streamOutputStream = new ServletOutputStream() {

							@Override
							public boolean isReady() {
								return os.isReady();
							}

							@Override
							public void setWriteListener(
									WriteListener writeListener) {
								os.setWriteListener(writeListener);
							}

							@Override
							public void write(int b) throws IOException {
								os.write(b);
								baos.write(b);
							}

							@Override
							public void flush() throws IOException {
								os.flush();
								if (baos != null) {
									byte[] bytes = baos.toByteArray();
									baos.close();
									baos = null;
									String encoding = getCharacterEncoding();
									if (encoding == null)
										encoding = "UTF-8";
									MDC.remove("method");
									MDC.remove("url");
									logger.info("\n{}", new String(bytes, 0,
											bytes.length, encoding));
								}
							}

						};
					}
				}
			}
			return streamOutputStream;

		}

	}

}
