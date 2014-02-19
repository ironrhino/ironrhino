package org.ironrhino.common.support;

import java.util.Date;
import java.util.concurrent.ExecutorService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ironrhino.common.service.PageViewService;
import org.ironrhino.core.servlet.AccessHandler;
import org.ironrhino.core.session.HttpSessionManager;
import org.ironrhino.core.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Integer.MIN_VALUE)
public class PageViewHandler implements AccessHandler {

	@Autowired(required = false)
	private PageViewService pageViewService;

	@Autowired(required = false)
	private ExecutorService executorService;

	@Autowired
	private HttpSessionManager httpSessionManager;

	@Override
	public String getPattern() {
		return null;
	}

	@Override
	public boolean handle(final HttpServletRequest request,
			HttpServletResponse response) {
		if (pageViewService != null
				&& request.getMethod().equalsIgnoreCase("GET")
				&& !request.getRequestURI().startsWith("/assets/")
				&& !request.getRequestURI().endsWith("/favicon.ico")) {
			final String remoteAddr = RequestUtils.getRemoteAddr(request);
			final String requestURL = request.getRequestURL().toString();
			final String sessionId = httpSessionManager.getSessionId(request);
			final String username = RequestUtils.getCookieValue(request, "U");
			final String referer = request.getHeader("Referer");
			Runnable task = new Runnable() {
				@Override
				public void run() {
					pageViewService.put(new Date(), remoteAddr, requestURL,
							sessionId, username, referer);
				}
			};
			if (executorService != null)
				executorService.execute(task);
			else
				task.run();
		}
		return false;
	}
}
