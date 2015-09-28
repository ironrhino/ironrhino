package org.ironrhino.core.session;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.RequestUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class HttpSessionFilter extends OncePerRequestFilter implements ApplicationContextAware {

	@Autowired
	private ServletContext servletContext;

	@Autowired
	private HttpSessionManager httpSessionManager;

	@Autowired(required = false)
	private List<HttpSessionFilterHook> httpSessionFilterHooks;

	private List<String> excludePatternsList;

	@Override
	public void setApplicationContext(ApplicationContext ctx) throws BeansException {
		String excludePatterns = ctx.getEnvironment()
				.getProperty(StringUtils.uncapitalize(getClass().getSimpleName()) + ".excludePatterns");
		if (StringUtils.isNotBlank(excludePatterns))
			excludePatternsList = Arrays.asList(excludePatterns.split("\\s*,\\s*"));
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String uri = RequestUtils.getRequestUri(request);
		if (excludePatternsList != null)
			for (String pattern : excludePatternsList)
				if (org.ironrhino.core.util.StringUtils.matchesWildcard(uri, pattern)) {
					chain.doFilter(request, response);
					return;
				}
		final WrappedHttpSession session = new WrappedHttpSession(request, response, servletContext,
				httpSessionManager);
		WrappedHttpServletRequest wrappedHttpRequest = new WrappedHttpServletRequest(request, session);
		final WrappedHttpServletResponse wrappedHttpResponse = new WrappedHttpServletResponse(response, session);
		if (httpSessionFilterHooks != null)
			try {
				for (HttpSessionFilterHook httpSessionFilterHook : httpSessionFilterHooks)
					httpSessionFilterHook.beforeDoFilter();
				chain.doFilter(wrappedHttpRequest, wrappedHttpResponse);
			} finally {
				for (int i = httpSessionFilterHooks.size() - 1; i > -1; i--) {
					HttpSessionFilterHook httpSessionFilterHook = httpSessionFilterHooks.get(i);
					httpSessionFilterHook.afterDoFilter();
				}
			}
		else
			chain.doFilter(wrappedHttpRequest, wrappedHttpResponse);
		if (!wrappedHttpRequest.isAsyncStarted()) {
			try {
				session.save();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				wrappedHttpResponse.commit();
			}
		} else {
			wrappedHttpRequest.getAsyncContext().addListener(new AsyncListener() {

				@Override
				public void onStartAsync(AsyncEvent event) throws IOException {
				}

				@Override
				public void onTimeout(AsyncEvent event) throws IOException {
				}

				@Override
				public void onError(AsyncEvent event) throws IOException {
				}

				@Override
				public void onComplete(AsyncEvent event) throws IOException {
					try {
						session.save();
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						wrappedHttpResponse.commit();
					}
				}
			});
		}

	}
}
