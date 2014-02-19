package org.ironrhino.core.session.impl;

import java.math.BigInteger;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.session.HttpSessionManager;
import org.ironrhino.core.session.HttpSessionStore;
import org.ironrhino.core.session.WrappedHttpSession;
import org.ironrhino.core.util.CodecUtils;
import org.ironrhino.core.util.NumberUtils;
import org.ironrhino.core.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("httpSessionManager")
public class DefaultHttpSessionManager implements HttpSessionManager {

	private static final String SALT = "awpeqaidasdfaioiaoduifayzuxyaaokadoaifaodiaoi";

	private static final String SESSION_TRACKER_SEPERATOR = "-";

	public static final int DEFAULT_LIFETIME = -1; // in seconds

	public static final int DEFAULT_MAXINACTIVEINTERVAL = 43200; // in seconds

	public static final int DEFAULT_MINACTIVEINTERVAL = 60;// in seconds

	@Value("${httpSessionManager.sessionTrackerName:"
			+ DEFAULT_SESSION_TRACKER_NAME + "}")
	private String sessionTrackerName = DEFAULT_SESSION_TRACKER_NAME;

	@Value("${httpSessionManager.supportSessionTrackerFromURL:false}")
	private boolean supportSessionTrackerFromURL;

	@Value("${httpSessionManager.localeCookieName:"
			+ DEFAULT_COOKIE_NAME_LOCALE + "}")
	private String localeCookieName = DEFAULT_COOKIE_NAME_LOCALE;

	@Value("${httpSessionManager.defaultLocaleName:}")
	private String defaultLocaleName;

	@Autowired
	@Qualifier("cookieBased")
	private HttpSessionStore cookieBased;

	@Autowired
	@Qualifier("cacheBased")
	private HttpSessionStore cacheBased;

	@Value("${httpSessionManager.lifetime:" + DEFAULT_LIFETIME + "}")
	private int lifetime;

	@Value("${httpSessionManager.maxInactiveInterval:"
			+ DEFAULT_MAXINACTIVEINTERVAL + "}")
	private int maxInactiveInterval;

	@Value("${httpSessionManager.minActiveInterval:"
			+ DEFAULT_MINACTIVEINTERVAL + "}")
	private int minActiveInterval;

	public String getDefaultLocaleName() {
		return defaultLocaleName;
	}

	public void setDefaultLocaleName(String defaultLocaleName) {
		this.defaultLocaleName = defaultLocaleName;
	}

	public int getMaxInactiveInterval() {
		return maxInactiveInterval;
	}

	public void setMaxInactiveInterval(int maxInactiveInterval) {
		this.maxInactiveInterval = maxInactiveInterval;
	}

	public int getMinActiveInterval() {
		return minActiveInterval;
	}

	public void setMinActiveInterval(int minActiveInterval) {
		this.minActiveInterval = minActiveInterval;
	}

	@Override
	public String getLocaleCookieName() {
		return localeCookieName;
	}

	@Override
	public String getSessionTrackerName() {
		return sessionTrackerName;
	}

	@Override
	public boolean supportSessionTrackerFromURL() {
		return supportSessionTrackerFromURL;
	}

	@Override
	public String getSessionId(HttpServletRequest request) {
		String sessionTracker = RequestUtils.getCookieValue(request,
				getSessionTrackerName());
		if (sessionTracker != null) {
			sessionTracker = CodecUtils.swap(sessionTracker);
			String[] array = sessionTracker.split(SESSION_TRACKER_SEPERATOR);
			return array[0];
		} else {
			String path = request.getRequestURI();
			if (path.indexOf(";") > -1) {
				path = path.substring(path.indexOf(";") + 1);
				if (path.startsWith(getSessionTrackerName() + "="))
					return path.substring(path.indexOf("=") + 1);
			}
		}
		return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void initialize(WrappedHttpSession session) {

		// simulated session
		Map<String, Object> sessionMap = (Map<String, Object>) session
				.getRequest().getAttribute(REQUEST_ATTRIBUTE_KEY_SESSION_MAP);
		if (sessionMap != null) {
			session.setAttrMap(sessionMap);
			return;
		}

		String sessionTracker = session.getSessionTracker();
		long now = session.getNow();
		String sessionId = null;
		long creationTime = now;
		long lastAccessedTime = now;

		if (StringUtils.isNotBlank(sessionTracker)) {
			sessionTracker = CodecUtils.swap(sessionTracker);
			if (session.isRequestedSessionIdFromURL()) {
				sessionId = sessionTracker;
			} else {
				try {
					String[] array = sessionTracker
							.split(SESSION_TRACKER_SEPERATOR);
					sessionId = array[0];
					if (array.length > 1)
						creationTime = NumberUtils.xToDecimal(62, array[1])
								.longValue();
					if (array.length > 2)
						lastAccessedTime = NumberUtils.xToDecimal(62, array[2])
								.longValue();
					boolean timeout = (lifetime > 0 && (now - creationTime > lifetime * 1000))
							|| (now - lastAccessedTime > maxInactiveInterval * 1000);
					if (timeout) {
						invalidate(session);
						return;
					}
				} catch (Exception e) {
					invalidate(session);
					return;
				}
			}
		} else {
			session.setNew(true);
			sessionId = CodecUtils.nextId(SALT);
		}
		session.setId(sessionId);
		session.setCreationTime(creationTime);
		session.setLastAccessedTime(lastAccessedTime);
		session.setMaxInactiveInterval(maxInactiveInterval);
		session.setMinActiveInterval(minActiveInterval);
		if (session.getSessionTracker() == null)
			session.setSessionTracker(getSessionTracker(session));
		doInitialize(session);
	}

	@Override
	public void save(WrappedHttpSession session) {
		// simulated session
		if (session.getRequest()
				.getAttribute(REQUEST_ATTRIBUTE_KEY_SESSION_MAP) != null) {
			session.getRequest().removeAttribute(
					REQUEST_ATTRIBUTE_KEY_SESSION_MAP);
			return;
		}
		boolean sessionTrackerChanged = false;
		if (session.isInvalid()) {
			sessionTrackerChanged = true;
		}
		if (session.isNew())
			sessionTrackerChanged = true;
		if (session.getNow() - session.getLastAccessedTime() > session
				.getMinActiveInterval() * 1000) {
			session.setLastAccessedTime(session.getNow());
			sessionTrackerChanged = true;
		}
		if (!session.isRequestedSessionIdFromURL() && sessionTrackerChanged) {
			// if (sessionTrackerChanged) {
			session.setSessionTracker(this.getSessionTracker(session));
			RequestUtils.saveCookie(session.getRequest(),
					session.getResponse(), getSessionTrackerName(),
					session.getSessionTracker(), true);
		}
		doSave(session);
	}

	@Override
	public void invalidate(WrappedHttpSession session) {
		session.setInvalid(true);
		session.getAttrMap().clear();
		if (!session.isRequestedSessionIdFromURL()) {
			RequestUtils.deleteCookie(session.getRequest(),
					session.getResponse(), getSessionTrackerName(), true);
		}
		doInvalidate(session);
		session.setId(CodecUtils.nextId(SALT));
		session.setCreationTime(session.getNow());
		session.setLastAccessedTime(session.getNow());
		session.setSessionTracker(this.getSessionTracker(session));
	}

	@Override
	public String getSessionTracker(WrappedHttpSession session) {
		if (session.isRequestedSessionIdFromURL())
			return session.getId();
		StringBuilder sb = new StringBuilder();
		sb.append(session.getId());
		sb.append(SESSION_TRACKER_SEPERATOR);
		sb.append(NumberUtils.decimalToX(62,
				BigInteger.valueOf(session.getCreationTime())));
		sb.append(SESSION_TRACKER_SEPERATOR);
		sb.append(NumberUtils.decimalToX(62,
				BigInteger.valueOf(session.getLastAccessedTime())));
		return CodecUtils.swap(sb.toString());
	}

	private void doInitialize(WrappedHttpSession session) {
		if (session.isRequestedSessionIdFromURL())
			cacheBased.initialize(session);
		else
			cookieBased.initialize(session);
	}

	private void doSave(WrappedHttpSession session) {
		if (session.isRequestedSessionIdFromURL())
			cacheBased.save(session);
		else
			cookieBased.save(session);

	}

	private void doInvalidate(WrappedHttpSession session) {
		if (session.isRequestedSessionIdFromURL())
			cacheBased.invalidate(session);
		else
			cookieBased.invalidate(session);
	}

	@Override
	public Locale getLocale(HttpServletRequest request) {
		String localeName = RequestUtils.getCookieValue(request,
				localeCookieName);
		if (StringUtils.isBlank(localeName))
			localeName = defaultLocaleName;
		if (StringUtils.isNotBlank(localeName))
			for (Locale locale : Locale.getAvailableLocales())
				if (localeName.equalsIgnoreCase(locale.toString()))
					return locale;
		return request.getLocale();
	}

}
