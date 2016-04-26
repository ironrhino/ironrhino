package org.ironrhino.core.servlet;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.DispatcherType;
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
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.CodecUtils;
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

	public static final String HTTP_HEADER_REQUEST_ID = "X-Request-Id";
	public static final String MDC_KEY_SESSION_ID = "sessionId";
	public static final String MDC_KEY_REQUEST_ID = "requestId";

	private Logger logger = LoggerFactory.getLogger(getClass());

	private Logger accessLog = LoggerFactory.getLogger("access");

	private Logger accesWarnLog = LoggerFactory.getLogger("access-warn");

	public static final long DEFAULT_RESPONSETIMETHRESHOLD = 5000;

	public static final boolean DEFAULT_PRINT = true;

	@Value("${accessFilter.responseTimeThreshold:" + DEFAULT_RESPONSETIMETHRESHOLD + "}")
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
			excludePatternsList = Arrays.asList(excludePatterns.split("\\s*,\\s*"));
	}

	@Override
	public void init(FilterConfig filterConfig) {
		_init();
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
			throws IOException, ServletException {
		boolean isRequestDispatcher = req.getDispatcherType() == DispatcherType.REQUEST;
		HttpServletRequest request = new ProxySupportHttpServletRequest((HttpServletRequest) req);
		HttpServletResponse response = (HttpServletResponse) resp;
		RequestContext.set(request, response);
		try {
			if (isRequestDispatcher && RequestUtils.isInternalTesting(request))
				response.addHeader("X-Instance-Id", AppInfo.getInstanceId());

			String uri = request.getRequestURI();
			uri = uri.substring(request.getContextPath().length());

			for (String pattern : excludePatternsList) {
				if (org.ironrhino.core.util.StringUtils.matchesWildcard(uri, pattern)) {
					chain.doFilter(req, resp);
					return;
				}
			}

			if (isRequestDispatcher && handlers != null)
				loop: for (AccessHandler handler : handlers) {
					String excludePattern = handler.getExcludePattern();
					if (StringUtils.isNotBlank(excludePattern)) {
						String[] arr = excludePattern.split("\\s*,\\s*");
						for (String pa : arr)
							if (org.ironrhino.core.util.StringUtils.matchesWildcard(uri, pa)) {
								continue loop;
							}
					}
					String pattern = handler.getPattern();
					boolean matched = StringUtils.isBlank(pattern);
					if (!matched) {
						String[] arr = pattern.split("\\s*,\\s*");
						for (String pa : arr)
							if (org.ironrhino.core.util.StringUtils.matchesWildcard(uri, pa)) {
								matched = true;
								break;
							}
					}
					if (matched) {
						if (handler.handle(request, response)) {
							return;
						}
					}
				}

			if (request.getAttribute("userAgent") == null)
				request.setAttribute("userAgent", new UserAgent(request.getHeader("User-Agent")));
			MDC.put("remoteAddr", request.getRemoteAddr());
			MDC.put("method", request.getMethod());
			StringBuffer url = request.getRequestURL();
			if (StringUtils.isNotBlank(request.getQueryString()))
				url.append('?').append(request.getQueryString());
			MDC.put("url", " " + url.toString());
			String s = request.getHeader("User-Agent");
			if (s == null)
				s = "";
			MDC.put("userAgent", " UserAgent:" + s);
			s = request.getHeader("Referer");
			if (s == null)
				s = "";
			MDC.put("referer", " Referer:" + s);
			s = RequestUtils.getCookieValue(request, DefaultAuthenticationSuccessHandler.COOKIE_NAME_LOGIN_USER);
			MDC.put("username", s != null ? " " + s : " ");
			String sessionId = null;
			if (httpSessionManager != null) {
				sessionId = httpSessionManager.getSessionId(request);
				if (sessionId != null) {
					MDC.put("session", " session:" + sessionId);
					MDC.put(MDC_KEY_SESSION_ID, sessionId);
				}
			}
			String requestId = (String) request.getAttribute(MDC_KEY_REQUEST_ID);
			if (requestId == null) {
				requestId = request.getHeader(HTTP_HEADER_REQUEST_ID);
				if (StringUtils.isBlank(requestId)) {
					requestId = CodecUtils.nextId();
					if (sessionId != null) {
						requestId = new StringBuilder(sessionId).append('.').append(requestId).toString();
					}
					response.setHeader(HTTP_HEADER_REQUEST_ID, requestId);
				}
				request.setAttribute(MDC_KEY_REQUEST_ID, requestId);
			}
			MDC.put("request", " request:" + requestId);
			MDC.put(MDC_KEY_REQUEST_ID, requestId);
			try {
				if (isRequestDispatcher && print && !uri.startsWith("/assets/") && !uri.startsWith("/remoting/")
						&& request.getHeader("Last-Event-Id") == null)
					accessLog.info("");

				long start = System.currentTimeMillis();
				chain.doFilter(request, response);
				long responseTime = System.currentTimeMillis() - start;
				if (isRequestDispatcher && responseTime > responseTimeThreshold) {
					StringBuilder sb = new StringBuilder();
					sb.append(RequestUtils.serializeData(request)).append(" response time:").append(responseTime)
							.append("ms");
					accesWarnLog.warn(sb.toString());
				}
			} catch (ServletException e) {
				logger.error(e.getMessage(), e);
				throw e;
			} finally {
				MDC.clear();
			}
		} finally {
			RequestContext.reset();
		}
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
