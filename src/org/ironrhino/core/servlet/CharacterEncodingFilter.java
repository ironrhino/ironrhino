package org.ironrhino.core.servlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.RequestUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import lombok.Setter;

public class CharacterEncodingFilter extends OncePerRequestFilter {

	public static final String PARAMETER_NAME_INPUT_ENCODING = "_input_encoding";

	public static final String PARAMETER_NAME_OUTPUT_ENCODING = "_output_encoding";

	private static final String[] BINARY_TYPES = "image,video,audio,font".split(",");

	private static final Map<String, String> CONTENT_TYPES_EXCLUDE_CHARSET; // Content-Type doesn't allow charset

	static {
		CONTENT_TYPES_EXCLUDE_CHARSET = new HashMap<>();
		CONTENT_TYPES_EXCLUDE_CHARSET.put("wasm", "application/wasm");
	}

	@Setter
	private String encoding;

	@Setter
	private boolean forceEncoding = false;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String uri = request.getRequestURI();
		if (uri.lastIndexOf('.') > uri.lastIndexOf('/')) {
			String contentType = request.getServletContext().getMimeType(uri);
			if (contentType != null) {
				for (String binaryType : BINARY_TYPES) {
					if (contentType.startsWith(binaryType)) {
						filterChain.doFilter(request, response);
						return;
					}
				}
			}
			String suffix = uri.substring(uri.lastIndexOf('.') + 1);
			contentType = CONTENT_TYPES_EXCLUDE_CHARSET.get(suffix);
			if (contentType != null) {
				response.setContentType(contentType);
				filterChain.doFilter(request, response);
				return;
			}
		}
		Map<String, String> map = RequestUtils.parseParametersFromQueryString(request.getQueryString());
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

		filterChain.doFilter(request, response);
	}

}
