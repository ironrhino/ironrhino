package org.ironrhino.core.session;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.IteratorEnumeration;
import org.ironrhino.core.util.RequestUtils;

public class WrappedHttpSession implements Serializable, HttpSession {

	private static final long serialVersionUID = -4227316119138095858L;

	public static Pattern SESSION_TRACKER_PATTERN;

	private String id;

	private transient HttpSessionManager httpSessionManager;

	private transient HttpServletRequest request;

	private transient HttpServletResponse response;

	private transient ServletContext context;

	private Map<String, Object> attrMap = new HashMap<String, Object>(8);

	private long creationTime;

	private long lastAccessedTime;

	private long now;

	private int maxInactiveInterval;

	private int minActiveInterval;

	private boolean dirty;

	private boolean isnew;

	private boolean fromCookie = true;

	/**
	 * sessionTracker -> id-creationTime-lastAccessedTime
	 */
	private String sessionTracker;

	private boolean invalid;

	private String requestURL;

	public WrappedHttpSession(HttpServletRequest request,
			HttpServletResponse response, ServletContext context,
			HttpSessionManager httpSessionManager) {
		now = System.currentTimeMillis();
		this.request = request;
		this.response = response;
		this.context = context;
		this.httpSessionManager = httpSessionManager;
		requestURL = request.getRequestURL().toString();
		sessionTracker = RequestUtils.getCookieValue(request,
				httpSessionManager.getSessionTrackerName());
		if (StringUtils.isBlank(sessionTracker)
				&& httpSessionManager.supportSessionTrackerFromURL()) {
			sessionTracker = (String) request
					.getAttribute(HttpSessionManager.REQUEST_ATTRIBUTE_SESSION_TRACKER_IN_URL);
			if (StringUtils.isBlank(sessionTracker))
				sessionTracker = request.getParameter(httpSessionManager
						.getSessionTrackerName());
			if (StringUtils.isBlank(sessionTracker)) {
				String requestURL = request.getRequestURL().toString();
				if (requestURL.indexOf(';') > -1) {
					requestURL = requestURL
							.substring(requestURL.indexOf(';') + 1);
					String[] array = requestURL.split(";");
					for (String pair : array) {
						if (pair.indexOf('=') > 0) {
							String k = pair.substring(0, pair.indexOf('='));
							if (k.equals(httpSessionManager
									.getSessionTrackerName())) {
								sessionTracker = pair.substring(pair
										.indexOf('=') + 1);
								break;
							}
						}
					}
				}
			}
			if (StringUtils.isNotBlank(sessionTracker)) {
				fromCookie = false;
			} else {
				String referer = request.getHeader("Referer");
				if (referer != null
						&& RequestUtils.isSameOrigin(requestURL, referer)) {
					fromCookie = false;
				}
			}
		}
		if (SESSION_TRACKER_PATTERN == null)
			SESSION_TRACKER_PATTERN = Pattern.compile(';'
					+ httpSessionManager.getSessionTrackerName() + "=.+");
		httpSessionManager.initialize(this);
	}

	public HttpSessionManager getHttpSessionManager() {
		return httpSessionManager;
	}

	public void save() {
		httpSessionManager.save(this);
	}

	public HttpServletRequest getRequest() {
		return request;
	}

	public HttpServletResponse getResponse() {
		return response;
	}

	public ServletContext getContext() {
		return context;
	}

	public Map<String, Object> getAttrMap() {
		return attrMap;
	}

	public void setAttrMap(Map<String, Object> attrMap) {
		this.attrMap = attrMap;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setAttribute(String key, Object object) {
		attrMap.put(key, object);
		dirty = true;
	}

	@Override
	public Object getAttribute(String key) {
		return attrMap.get(key);
	}

	@Override
	public void removeAttribute(String key) {
		attrMap.remove(key);
		dirty = true;
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		return new IteratorEnumeration<String>(attrMap.keySet().iterator());
	}

	@Override
	public long getCreationTime() {
		return this.creationTime;
	}

	@Override
	public void invalidate() {
		httpSessionManager.invalidate(this);
	}

	@Override
	public boolean isNew() {
		return isnew;
	}

	public void setNew(boolean isnew) {
		this.isnew = isnew;
	}

	@Override
	public long getLastAccessedTime() {
		return lastAccessedTime;
	}

	@Override
	public ServletContext getServletContext() {
		return context;
	}

	@Override
	public void setMaxInactiveInterval(int arg0) {
		maxInactiveInterval = arg0;
	}

	@Override
	public int getMaxInactiveInterval() {
		return maxInactiveInterval;
	}

	public int getMinActiveInterval() {
		return minActiveInterval;
	}

	public void setMinActiveInterval(int minActiveInterval) {
		this.minActiveInterval = minActiveInterval;
	}

	public long getNow() {
		return now;
	}

	public void setCreationTime(long creationTime) {
		this.creationTime = creationTime;
	}

	public void setLastAccessedTime(long lastAccessedTime) {
		this.lastAccessedTime = lastAccessedTime;
	}

	public boolean isDirty() {
		return dirty;
	}

	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

	public String getSessionTracker() {
		return sessionTracker;
	}

	public void setSessionTracker(String sessionTracker) {
		this.sessionTracker = sessionTracker;
	}

	public void setId(String id) {
		this.id = id;
	}

	public boolean isInvalid() {
		return invalid;
	}

	public void setInvalid(boolean invalid) {
		this.invalid = invalid;
	}

	public boolean isRequestedSessionIdFromCookie() {
		return fromCookie;
	}

	public boolean isRequestedSessionIdFromURL() {
		return !fromCookie;
	}

	public String encodeURL(String url) {
		if (!isRequestedSessionIdFromURL() || StringUtils.isBlank(url)
				|| !RequestUtils.isSameOrigin(requestURL, url))
			return url;
		Matcher m = SESSION_TRACKER_PATTERN.matcher(url);
		if (m.find())
			return url;
		return doEncodeURL(url);
	}

	public String encodeRedirectURL(String url) {
		if (!isRequestedSessionIdFromURL() || StringUtils.isBlank(url))
			return url;
		Matcher m = SESSION_TRACKER_PATTERN.matcher(url);
		url = m.replaceAll("");
		return doEncodeURL(url);
	}

	private String doEncodeURL(String url) {
		String[] array = url.split("\\?", 2);
		StringBuilder sb = new StringBuilder();
		sb.append(array[0]);
		if (sessionTracker != null) {
			sb.append(';');
			sb.append(httpSessionManager.getSessionTrackerName());
			sb.append('=');
			sb.append(sessionTracker);
		}
		if (array.length == 2) {
			sb.append('?');
			sb.append(array[1]);
		}
		return sb.toString();
	}

	@Override
	@Deprecated
	public String[] getValueNames() {
		List<String> names = new ArrayList<String>();

		for (Enumeration<String> e = getAttributeNames(); e.hasMoreElements();) {
			names.add(e.nextElement());
		}

		return names.toArray(new String[names.size()]);
	}

	@Override
	@Deprecated
	public Object getValue(String key) {
		return getAttribute(key);
	}

	@Override
	@Deprecated
	public void removeValue(String key) {
		removeAttribute(key);
	}

	@Override
	@Deprecated
	public void putValue(String key, Object object) {
		setAttribute(key, object);
	}

	@Override
	@Deprecated
	public javax.servlet.http.HttpSessionContext getSessionContext() {
		throw new UnsupportedOperationException(
				"No longer supported method: getSessionContext");
	}

}
