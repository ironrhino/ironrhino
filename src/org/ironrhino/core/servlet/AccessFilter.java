package org.ironrhino.core.servlet;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.session.HttpSessionManager;
import org.ironrhino.core.spring.security.DefaultAuthenticationSuccessHandler;
import org.ironrhino.core.util.HttpClientUtils;
import org.ironrhino.core.util.RequestUtils;
import org.ironrhino.core.util.UserAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AccessFilter implements Filter {

	private Logger accessLog = LoggerFactory.getLogger("access");

	private Logger accesWarnLog = LoggerFactory.getLogger("access-warn");

	public static final long DEFAULT_RESPONSETIMETHRESHOLD = 5000;

	public static final boolean DEFAULT_PRINT = true;

	@Value("${accessFilter.responseTimeThreshold:"
			+ DEFAULT_RESPONSETIMETHRESHOLD + "}")
	public long responseTimeThreshold = DEFAULT_RESPONSETIMETHRESHOLD;

	@Value("${accessFilter.print:" + DEFAULT_PRINT + "}")
	private boolean print = DEFAULT_PRINT;

	@Value("${accessFilter.excludePatterns:}")
	private String excludePatterns;

	private List<String> excludePatternsList = Collections.emptyList();

	@Autowired(required = false)
	private List<AccessHandler> handlers;

	@Autowired
	private HttpSessionManager httpSessionManager;

	public void setExcludePatterns(String excludePatterns) {
		this.excludePatterns = excludePatterns;
	}

	public void setPrint(boolean print) {
		this.print = print;
	}

	public long getResponseTimeThreshold() {
		return responseTimeThreshold;
	}

	public void setResponseTimeThreshold(long responseTimeThreshold) {
		this.responseTimeThreshold = responseTimeThreshold;
	}

	@PostConstruct
	public void _init() {
		if (StringUtils.isNotBlank(excludePatterns))
			excludePatternsList = Arrays.asList(excludePatterns
					.split("\\s*,\\s*"));
	}

	@Override
	public void init(FilterConfig filterConfig) {
		_init();
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp,
			FilterChain chain) throws IOException, ServletException {

		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) resp;

		String uri = request.getRequestURI();
		uri = uri.substring(request.getContextPath().length());
		for (String pattern : excludePatternsList) {
			if (org.ironrhino.core.util.StringUtils.matchesWildcard(uri,
					pattern)) {
				chain.doFilter(req, resp);
				return;
			}
		}

		if (request.getAttribute("userAgent") == null)
			request.setAttribute("userAgent",
					new UserAgent(request.getHeader("User-Agent")));
		MDC.put("remoteAddr", RequestUtils.getRemoteAddr(request));
		MDC.put("method", request.getMethod());
		StringBuffer url = request.getRequestURL();
		if (StringUtils.isNotBlank(request.getQueryString()))
			url.append('?').append(request.getQueryString());
		MDC.put("url", " " + url.toString());
		String s = request.getHeader("User-Agent");
		if (s != null)
			MDC.put("userAgent", " UserAgent:" + s);
		s = request.getHeader("Referer");
		if (s != null)
			MDC.put("referer", " Referer:" + s);
		s = RequestUtils.getCookieValue(request,
				DefaultAuthenticationSuccessHandler.COOKIE_NAME_LOGIN_USER);
		MDC.put("username", s != null ? " " + s : " ");
		if (httpSessionManager != null) {
			String sessionId = httpSessionManager.getSessionId(request);
			if (sessionId != null)
				MDC.put("session", " session:" + sessionId);
		}
		if (print && !uri.startsWith("/assets/")
				&& !uri.startsWith("/remoting/")
				&& request.getHeader("Last-Event-Id") == null)
			accessLog.info("");

		if (handlers != null)
			for (AccessHandler handler : handlers) {
				String pattern = handler.getPattern();
				boolean matched = StringUtils.isBlank(pattern);
				if (!matched) {
					String[] arr = pattern.split("\\s*,\\s*");
					for (String pa : arr)
						if (org.ironrhino.core.util.StringUtils
								.matchesWildcard(uri, pa)) {
							matched = true;
							break;
						}
				}
				if (matched) {
					if (handler.handle(request, response)) {
						MDC.clear();
						return;
					}
				}
			}

		long start = System.currentTimeMillis();
		chain.doFilter(req, resp);
		long responseTime = System.currentTimeMillis() - start;
		if (responseTime > responseTimeThreshold) {
			StringBuilder sb = new StringBuilder();
			sb.append(RequestUtils.serializeData(request))
					.append(" response time:").append(responseTime)
					.append("ms");
			accesWarnLog.warn(sb.toString());
		}
		MDC.clear();
	}

	@Override
	public void destroy() {
		try {
			HttpClientUtils.getDefaultInstance().close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
