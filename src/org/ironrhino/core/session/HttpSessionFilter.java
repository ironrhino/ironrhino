package org.ironrhino.core.session;

import java.io.IOException;
import java.util.List;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class HttpSessionFilter extends OncePerRequestFilter {

	@Autowired
	private HttpSessionManager httpSessionManager;

	@Autowired(required = false)
	private List<HttpSessionFilterHook> httpSessionFilterHooks;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		final WrappedHttpSession session = new WrappedHttpSession(request, response, getServletContext(),
				httpSessionManager);
		WrappedHttpServletRequest wrappedHttpRequest = new WrappedHttpServletRequest(request, session);
		final WrappedHttpServletResponse wrappedHttpResponse = new WrappedHttpServletResponse(response, session);
		if (httpSessionFilterHooks != null) {
			int appliedHooks = 0;
			try {
				boolean skipFilter = false;
				for (HttpSessionFilterHook httpSessionFilterHook : httpSessionFilterHooks) {
					if (httpSessionFilterHook.beforeFilterChain(wrappedHttpRequest, wrappedHttpResponse)) {
						skipFilter = true;
						break;
					} else {
						appliedHooks++;
					}
				}
				if (!skipFilter)
					chain.doFilter(wrappedHttpRequest, wrappedHttpResponse);
			} finally {
				for (int i = 0; i < appliedHooks; i++) {
					HttpSessionFilterHook httpSessionFilterHook = httpSessionFilterHooks.get(i);
					httpSessionFilterHook.afterFilterChain(wrappedHttpRequest, wrappedHttpResponse);
				}
			}
		} else {
			chain.doFilter(wrappedHttpRequest, wrappedHttpResponse);
		}
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
