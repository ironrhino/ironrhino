package org.ironrhino.core.session;

import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.security.util.RC4;
import org.ironrhino.core.util.RequestUtils;

public class WrappedHttpServletRequest extends HttpServletRequestWrapper {

	private WrappedHttpSession session;

	private volatile Map<String, String[]> parameterMap;

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

	@Override
	public String getParameter(String name) {
		String[] values = getParameterMap().get(name);
		return values != null ? values[0] : null;
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		Map<String, String[]> temp = parameterMap;
		if (temp == null) {
			synchronized (this) {
				temp = parameterMap;
				if (temp == null) {
					Map<String, String[]> map = super.getParameterMap();
					String key = RequestUtils.getCookieValue(session.getRequest(), "X");
					for (Map.Entry<String, String[]> entry : map.entrySet()) {
						String name = entry.getKey();
						String[] value = entry.getValue();
						if (StringUtils.isNotBlank(key) && name.toLowerCase(Locale.ROOT).endsWith("password")
								&& value != null) {
							for (int i = 0; i < value.length; i++) {
								if (value[i].matches("\\p{XDigit}+")) {
									try {
										if (key.length() > 10)
											key = key.substring(key.length() - 10, key.length());
										String str = RC4.decryptWithKey(value[i], key);
										if (str.endsWith(key))
											value[i] = str.substring(0, str.length() - key.length());
									} catch (IllegalArgumentException e) {
										e.printStackTrace();
									} catch (Exception e) {
										e.printStackTrace();
									}
								}
							}
						}
					}
					if (key != null)
						RequestUtils.deleteCookie(session.getRequest(), session.getResponse(), "X");
					parameterMap = temp = map;
				}
			}
		}
		return temp;
	}

	@Override
	public String[] getParameterValues(String name) {
		return getParameterMap().get(name);
	}

}
