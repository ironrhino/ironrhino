package org.ironrhino.core.servlet;

import java.io.IOException;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.RequestUtils;
import org.springframework.web.filter.OncePerRequestFilter;

public class MethodAndEncodingFilter extends OncePerRequestFilter {

	public static final String PARAMETER_NAME_METHOD = "_method";

	public static final String PARAMETER_NAME_INPUT_ENCODING = "_input_encoding";

	public static final String PARAMETER_NAME_OUTPUT_ENCODING = "_output_encoding";

	private String encoding;

	private boolean forceEncoding = false;

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public void setForceEncoding(boolean forceEncoding) {
		this.forceEncoding = forceEncoding;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request,
			HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		Map<String, String> map = RequestUtils
				.parseParametersFromQueryString(request.getQueryString());
		String inputEncoding = map.get(PARAMETER_NAME_INPUT_ENCODING);
		String outputEncoding = map.get(PARAMETER_NAME_OUTPUT_ENCODING);
		if (StringUtils.isNotBlank(inputEncoding))
			request.setCharacterEncoding(inputEncoding);
		else if (this.encoding != null && this.forceEncoding)
			request.setCharacterEncoding(this.encoding);

		if (StringUtils.isNotBlank(outputEncoding))
			response.setCharacterEncoding(outputEncoding);
		else if (this.encoding != null && this.forceEncoding)
			response.setCharacterEncoding(this.encoding);

		String method = map.get(PARAMETER_NAME_METHOD);
		if (StringUtils.isNotBlank(method))
			request = new WrappedHttpServletRequest(request, method.trim()
					.toUpperCase());
		if (StringUtils.isNotBlank(outputEncoding))
			response = new WrappedHttpServletResponse(response, outputEncoding);
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

	}

	private static class WrappedHttpServletResponse extends
			HttpServletResponseWrapper {

		private String encoding;

		public WrappedHttpServletResponse(HttpServletResponse response,
				String encoding) {
			super(response);
			this.encoding = encoding;
		}

		@Override
		public void setCharacterEncoding(String encoding) {
			this.encoding = encoding;
		}

		@Override
		public String getCharacterEncoding() {
			return this.encoding;
		}

	}

}
