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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("httpSessionManager")
public class DefaultHttpSessionManager implements HttpSessionManager {

	private static final String SALT = "awpeqaidasdfaioiaoduifayzuxyaaokadoaifaodiaoi";

	private static final String SESSION_KEY_REMOTE_ADDR = "_REMOTE_ADDR";

	private static final String SESSION_TRACKER_SEPERATOR = "-";

	protected Logger log = LoggerFactory.getLogger(this.getClass());

	@Value("${httpSessionManager.sessionTrackerName:" + DEFAULT_SESSION_TRACKER_NAME + "}")
	private String sessionTrackerName = DEFAULT_SESSION_TRACKER_NAME;

	@Value("${httpSessionManager.supportSessionTrackerFromURL:false}")
	private boolean supportSessionTrackerFromURL;

	@Value("${httpSessionManager.localeCookieName:" + DEFAULT_COOKIE_NAME_LOCALE + "}")
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

	@Value("${httpSessionManager.maxInactiveInterval:" + DEFAULT_MAXINACTIVEINTERVAL + "}")
	private int maxInactiveInterval;

	@Value("${httpSessionManager.minActiveInterval:" + DEFAULT_MINACTIVEINTERVAL + "}")
	private int minActiveInterval;

	@Value("${httpSessionManager.checkRemoteAddr:true}")
	private boolean checkRemoteAddr;

	@Value("${httpSessionManager.alwaysUseCacheBased:false}")
	private boolean alwaysUseCacheBased;

	@Value("${globalCookie:false}")
	private boolean globalCookie;

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
		String token = (String) request.getAttribute(REQUEST_ATTRIBUTE_KEY_SESSION_ID_FOR_API);
		if (token != null)
			return token;
		String sessionTracker = RequestUtils.getCookieValue(request, getSessionTrackerName());
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
		Map<String, Object> sessionMap = (Map<String, Object>) session.getRequest()
				.getAttribute(REQUEST_ATTRIBUTE_KEY_SESSION_MAP_FOR_API);
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
			if (session.isRequestedSessionIdFromURL() || alwaysUseCacheBased) {
				sessionId = sessionTracker;
			} else {
				sessionTracker = CodecUtils.swap(sessionTracker);
				try {
					String[] array = sessionTracker.split(SESSION_TRACKER_SEPERATOR);
					if (array.length == 1) {
						session.setNew(true);
						sessionId = CodecUtils.nextId(SALT);
					} else {
						sessionId = array[0];
						if (array.length > 1)
							creationTime = NumberUtils.xToDecimal(62, array[1]).longValue();
						if (array.length > 2)
							lastAccessedTime = NumberUtils.xToDecimal(62, array[2]).longValue();
						boolean timeout = (lifetime > 0 && (now - creationTime > lifetime * 1000))
								|| (now - lastAccessedTime > maxInactiveInterval * 1000);
						if (timeout) {
							invalidate(session);
							return;
						}
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
		sessionMap = (Map<String, Object>) session.getRequest().getAttribute(REQUEST_ATTRIBUTE_KEY_SESSION_MAP_FOR_SSO);
		if (sessionMap != null) {
			session.setAttrMap(sessionMap);
			session.setDirty(true);
		} else {
			doInitialize(session);
			if (checkRemoteAddr) {
				String addr = (String) session.getAttribute(SESSION_KEY_REMOTE_ADDR);
				if (addr != null && !session.getRequest().getRemoteAddr().equals(addr)) {
					log.warn("Invalidate session[{}] that created from {} but hijacked from {}", session.getId(), addr,
							session.getRequest().getRemoteAddr());
					invalidate(session);
				}
			}
		}
	}

	@Override
	public void save(WrappedHttpSession session) {
		// simulated session
		if (session.getRequest().getAttribute(REQUEST_ATTRIBUTE_KEY_SESSION_MAP_FOR_API) != null) {
			session.getRequest().removeAttribute(REQUEST_ATTRIBUTE_KEY_SESSION_MAP_FOR_API);
			return;
		}
		boolean sessionTrackerChanged = false;
		if (session.isInvalid()) {
			sessionTrackerChanged = true;
		}
		if (session.isNew())
			sessionTrackerChanged = true;
		if (session.getNow() - session.getLastAccessedTime() > session.getMinActiveInterval() * 1000) {
			session.setLastAccessedTime(session.getNow());
			sessionTrackerChanged = true;
		}
		if (!session.isRequestedSessionIdFromURL() && sessionTrackerChanged) {
			// if (sessionTrackerChanged) {
			session.setSessionTracker(this.getSessionTracker(session));
			RequestUtils.saveCookie(session.getRequest(), session.getResponse(), getSessionTrackerName(),
					session.getSessionTracker(), globalCookie);
		}
		if (checkRemoteAddr) {
			if (!session.getAttrMap().isEmpty() && session.getAttribute(SESSION_KEY_REMOTE_ADDR) == null)
				session.setAttribute(SESSION_KEY_REMOTE_ADDR, session.getRequest().getRemoteAddr());
		}
		doSave(session);
	}

	@Override
	public void invalidate(WrappedHttpSession session) {
		session.setInvalid(true);
		session.getAttrMap().clear();
		if (!session.isRequestedSessionIdFromURL() || alwaysUseCacheBased) {
			RequestUtils.deleteCookie(session.getRequest(), session.getResponse(), getSessionTrackerName(), true);
		}
		doInvalidate(session);
		session.setId(CodecUtils.nextId(SALT));
		session.setCreationTime(session.getNow());
		session.setLastAccessedTime(session.getNow());
		session.setSessionTracker(this.getSessionTracker(session));
	}

	@Override
	public String getSessionTracker(WrappedHttpSession session) {
		String token = (String) session.getRequest().getAttribute(REQUEST_ATTRIBUTE_KEY_SESSION_ID_FOR_API);
		if (token != null)
			return token;
		if (session.isRequestedSessionIdFromURL() || alwaysUseCacheBased)
			return session.getId();
		StringBuilder sb = new StringBuilder();
		sb.append(session.getId());
		sb.append(SESSION_TRACKER_SEPERATOR);
		sb.append(NumberUtils.decimalToX(62, BigInteger.valueOf(session.getCreationTime())));
		sb.append(SESSION_TRACKER_SEPERATOR);
		sb.append(NumberUtils.decimalToX(62, BigInteger.valueOf(session.getLastAccessedTime())));
		return CodecUtils.swap(sb.toString());
	}

	private void doInitialize(WrappedHttpSession session) {
		if (session.isRequestedSessionIdFromURL() || alwaysUseCacheBased)
			cacheBased.initialize(session);
		else
			cookieBased.initialize(session);
	}

	private void doSave(WrappedHttpSession session) {
		if ("Upgrade".equalsIgnoreCase(session.getRequest().getHeader("Connection"))) // websocket
			return;
		if (session.isRequestedSessionIdFromURL() || alwaysUseCacheBased)
			cacheBased.save(session);
		else
			cookieBased.save(session);

	}

	private void doInvalidate(WrappedHttpSession session) {
		if ("Upgrade".equalsIgnoreCase(session.getRequest().getHeader("Connection"))) // websocket
			return;
		if (session.isRequestedSessionIdFromURL() || alwaysUseCacheBased)
			cacheBased.invalidate(session);
		else
			cookieBased.invalidate(session);
	}

	@Override
	public Locale getLocale(HttpServletRequest request) {
		String localeName = RequestUtils.getCookieValue(request, localeCookieName);
		if (StringUtils.isBlank(localeName))
			localeName = defaultLocaleName;
		if (StringUtils.isNotBlank(localeName))
			for (Locale locale : Locale.getAvailableLocales())
				if (localeName.equalsIgnoreCase(locale.toString()))
					return locale;
		return request.getLocale();
	}

}
