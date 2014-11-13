package org.ironrhino.core.session;

import java.io.IOException;
import java.util.List;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HttpSessionFilter implements Filter {

	private static final String APPLIED_KEY = "APPLIED."
			+ HttpSessionFilter.class.getName();

	public static final String KEY_EXCLUDE_PATTERNS = "excludePatterns";

	public static final String DEFAULT_EXCLUDE_PATTERNS = "/assets/*,/remoting/*";

	private ServletContext servletContext;

	@Autowired
	private HttpSessionManager httpSessionManager;

	@Autowired(required = false)
	private List<HttpSessionFilterHook> httpSessionFilterHooks;

	private String[] excludePatterns;

	@Override
	public void init(FilterConfig filterConfig) {
		servletContext = filterConfig.getServletContext();
		String str = filterConfig.getInitParameter(KEY_EXCLUDE_PATTERNS);
		if (StringUtils.isBlank(str))
			str = DEFAULT_EXCLUDE_PATTERNS;
		excludePatterns = str.split("\\s*,\\s*");
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		if (req.getAttribute(APPLIED_KEY) != null) {
			chain.doFilter(request, response);
			return;
		}
		request.setAttribute(APPLIED_KEY, true);

		for (String pattern : excludePatterns) {
			String path = req.getRequestURI();
			path = path.substring(req.getContextPath().length());
			if (org.ironrhino.core.util.StringUtils.matchesWildcard(path,
					pattern)) {
				chain.doFilter(request, response);
				return;
			}
		}
		WrappedHttpSession session = new WrappedHttpSession(
				(HttpServletRequest) request, (HttpServletResponse) response,
				servletContext, httpSessionManager);
		WrappedHttpServletRequest wrappedHttpRequest = new WrappedHttpServletRequest(
				req, session);
		final WrappedHttpServletResponse wrappedHttpResponse = new WrappedHttpServletResponse(
				(HttpServletResponse) response, session);
		if (httpSessionFilterHooks != null)
			try {
				for (HttpSessionFilterHook httpSessionFilterHook : httpSessionFilterHooks)
					httpSessionFilterHook.beforeDoFilter();
				chain.doFilter(wrappedHttpRequest, wrappedHttpResponse);
			} finally {
				for (int i = httpSessionFilterHooks.size() - 1; i > -1; i--) {
					HttpSessionFilterHook httpSessionFilterHook = httpSessionFilterHooks
							.get(i);
					httpSessionFilterHook.afterDoFilter();
				}
			}
		else
			chain.doFilter(wrappedHttpRequest, wrappedHttpResponse);
		try {
			session.save();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (!wrappedHttpRequest.isAsyncStarted()) {
				wrappedHttpResponse.commit();
			} else {
				wrappedHttpRequest.getAsyncContext().addListener(
						new AsyncListener() {

							@Override
							public void onTimeout(AsyncEvent event)
									throws IOException {
							}

							@Override
							public void onStartAsync(AsyncEvent event)
									throws IOException {
							}

							@Override
							public void onError(AsyncEvent event)
									throws IOException {
							}

							@Override
							public void onComplete(AsyncEvent event)
									throws IOException {
								wrappedHttpResponse.commit();
							}
						});
			}
		}
	}

	@Override
	public void destroy() {

	}
}
