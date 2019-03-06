package org.ironrhino.core.session;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;

public class WrappedHttpServletRequest extends HttpServletRequestWrapper {

	private WrappedHttpSession session;

	public WrappedHttpServletRequest(HttpServletRequest request, WrappedHttpSession session) {
		super(request);
		this.session = session;
	}

	@Override
	public HttpSession getSession() {
		return session;
	}

	@Override
	public HttpSession getSession(boolean create) {
		return session;
	}

	@Override
	public boolean isRequestedSessionIdFromCookie() {
		return session.isRequestedSessionIdFromCookie();
	}

	@Override
	public boolean isRequestedSessionIdFromURL() {
		return session.isRequestedSessionIdFromURL();
	}

	@Override
	@Deprecated
	public boolean isRequestedSessionIdFromUrl() {
		return isRequestedSessionIdFromURL();
	}

	@Override
	public boolean isRequestedSessionIdValid() {
		return true;
	}

	@Override
	public String getRequestedSessionId() {
		return session.getId();
	}

	@Override
	public String changeSessionId() {
		return session.getHttpSessionManager().changeSessionId(session);
	}

	@Override
	public Locale getLocale() {
		Locale locale = session.getHttpSessionManager().getLocale((HttpServletRequest) this.getRequest());
		return locale;
	}

}
