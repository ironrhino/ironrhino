package org.ironrhino.core.servlet;

import java.io.IOException;
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

	@Setter
	private String encoding;

	@Setter
	private boolean forceEncoding = false;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
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
