package org.ironrhino.core.session.impl;

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.cache.CacheManager;
import org.ironrhino.core.session.HttpSessionStore;
import org.ironrhino.core.session.SessionCompressorManager;
import org.ironrhino.core.session.WrappedHttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("cacheBased")
public class CacheBasedHttpSessionStore implements HttpSessionStore {

	public static final String CACHE_NAMESPACE = "session";

	@Autowired
	private SessionCompressorManager sessionCompressorManager;

	@Autowired
	private CacheManager cacheManager;

	public void setCacheManager(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	@Override
	public void initialize(WrappedHttpSession session) {
		String sessionString;
		if (!cacheManager.supportsTimeToIdle()
				&& cacheManager.supportsUpdateTimeToLive())
			sessionString = (String) cacheManager.get(session.getId(),
					CACHE_NAMESPACE, session.getMaxInactiveInterval(),
					TimeUnit.SECONDS);
		else
			sessionString = (String) cacheManager.get(session.getId(),
					CACHE_NAMESPACE);
		sessionCompressorManager.uncompress(session, sessionString);
	}

	@Override
	public void save(WrappedHttpSession session) {
		String sessionString = sessionCompressorManager.compress(session);
		if (session.isDirty() && StringUtils.isBlank(sessionString)) {
			cacheManager.delete(session.getId(), CACHE_NAMESPACE);
			return;
		}
		if (cacheManager.supportsTimeToIdle()) {
			if (session.isDirty())
				cacheManager.put(session.getId(), sessionString,
						session.getMaxInactiveInterval(), -1, TimeUnit.SECONDS,
						CACHE_NAMESPACE);
		} else if (cacheManager.supportsUpdateTimeToLive()) {
			if (session.isDirty())
				cacheManager.put(session.getId(), sessionString, -1,
						session.getMaxInactiveInterval(), TimeUnit.SECONDS,
						CACHE_NAMESPACE);
		} else {
			if (session.isDirty()
					|| session.getNow() - session.getLastAccessedTime() > session
							.getMinActiveInterval() * 1000)
				cacheManager.put(session.getId(), sessionString,
						session.getMaxInactiveInterval(), TimeUnit.SECONDS,
						CACHE_NAMESPACE);
		}
	}

	@Override
	public void invalidate(WrappedHttpSession session) {
		cacheManager.delete(session.getId(), CACHE_NAMESPACE);
	}

}
