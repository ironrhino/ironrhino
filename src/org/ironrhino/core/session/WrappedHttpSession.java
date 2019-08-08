package org.ironrhino.core.session;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.ironrhino.core.util.IteratorEnumeration;
import org.ironrhino.core.util.RequestUtils;

public class WrappedHttpSession implements Serializable, HttpSession {

	private static final long serialVersionUID = -4227316119138095858L;

	private String id;

	private transient HttpSessionManager httpSessionManager;

	private transient HttpServletRequest request;

	private transient HttpServletResponse response;

	private transient ServletContext context;

	private volatile Map<String, Object> attrMap;

	private long creationTime;

	private long lastAccessedTime;

	private long now;

	private int maxInactiveInterval;

	private int minActiveInterval;

	private boolean isnew;

	private boolean cacheBased;

	/**
	 * sessionTracker -> id-creationTime-lastAccessedTime
	 */
	private String sessionTracker;

	private boolean invalid;

	public WrappedHttpSession(HttpServletRequest request, HttpServletResponse response, ServletContext context,
			HttpSessionManager httpSessionManager) {
		now = System.currentTimeMillis();
		this.request = request;
		this.response = response;
		this.context = context;
		this.httpSessionManager = httpSessionManager;
		sessionTracker = RequestUtils.getCookieValue(request, httpSessionManager.getSessionTrackerName());
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

	public Map<String, Object> getAttrMap(boolean create) {
		if (create && attrMap == null)
			attrMap = new HashMap<>(8);
		return attrMap;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setAttribute(String key, Object object) {
		getAttrMap(true).put(key, object);
		markAsDirty();
	}

	@Override
	public Object getAttribute(String key) {
		if (attrMap == null)
			return null;
		return attrMap.get(key);
	}

	@Override
	public void removeAttribute(String key) {
		if (attrMap == null)
			return;
		attrMap.remove(key);
		markAsDirty();
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		if (attrMap == null)
			return Collections.emptyEnumeration();
		return new IteratorEnumeration<>(attrMap.keySet().iterator());
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

	public void markAsNew() {
		this.isnew = true;
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
		return Boolean.TRUE.equals(request.getAttribute(HttpSessionManager.REQUEST_ATTRIBUTE_SESSION_MARK_AS_DIRTY));
	}

	public void markAsDirty() {
		request.setAttribute(HttpSessionManager.REQUEST_ATTRIBUTE_SESSION_MARK_AS_DIRTY, true);
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

	public void markAsInvalid() {
		this.invalid = true;
		attrMap = null;
	}

	public boolean isRequestedSessionIdFromCookie() {
		return true;
	}

	public boolean isRequestedSessionIdFromURL() {
		return false;
	}

	public boolean isCacheBased() {
		return cacheBased;
	}

	public void setCacheBased(boolean cacheBased) {
		this.cacheBased = cacheBased;
	}

	@Override
	@Deprecated
	public String[] getValueNames() {
		List<String> names = new ArrayList<>();

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
		throw new UnsupportedOperationException("No longer supported method: getSessionContext");
	}

}
