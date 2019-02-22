package org.ironrhino.core.session.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.cache.CacheManager;
import org.ironrhino.core.session.HttpSessionStore;
import org.ironrhino.core.session.SessionCompressorManager;
import org.ironrhino.core.session.WrappedHttpSession;
import org.ironrhino.core.util.DateUtils;
import org.ironrhino.core.util.ErrorMessage;
import org.ironrhino.core.util.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component("cacheBased")
@Slf4j
public class CacheBasedHttpSessionStore implements HttpSessionStore {

	public static final String CACHE_NAMESPACE = "session";

	private static final String SESSION_KEY_KICKED_OUT_FROM = "_KICKED_OUT_FROM";
	private static final String SESSION_KEY_KICKED_OUT_DATE = "_KICKED_OUT_DATE";

	private CacheManager cacheManager;

	@Autowired
	private SessionCompressorManager sessionCompressorManager;

	@Value("${httpSessionManager.maximumSessions:0}")
	private int maximumSessions;

	@Autowired
	public CacheBasedHttpSessionStore(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	@Override
	public void initialize(WrappedHttpSession session) {
		session.setCacheBased(true);
		String sessionString;
		if (!cacheManager.supportsTti() && cacheManager.supportsUpdateTtl())
			sessionString = (String) cacheManager.getWithTti(session.getId(), CACHE_NAMESPACE,
					session.getMaxInactiveInterval(), TimeUnit.SECONDS);
		else
			sessionString = (String) cacheManager.get(session.getId(), CACHE_NAMESPACE);
		sessionCompressorManager.uncompress(session, sessionString);
		if (maximumSessions > 0 && session.getAttribute(SESSION_KEY_KICKED_OUT_FROM) != null) {
			String ip = (String) session.getAttribute(SESSION_KEY_KICKED_OUT_FROM);
			String date = (String) session.getAttribute(SESSION_KEY_KICKED_OUT_DATE);
			invalidate(session);
			throw new ErrorMessage("kicked.out", new String[] { date, ip });
		}
	}

	@Override
	public void save(WrappedHttpSession session) {
		String sessionString = sessionCompressorManager.compress(session);
		if (session.isDirty() && StringUtils.isBlank(sessionString)) {
			cacheManager.delete(session.getId(), CACHE_NAMESPACE);
			return;
		}
		if (cacheManager.supportsTti()) {
			if (session.isDirty())
				cacheManager.putWithTti(session.getId(), sessionString, session.getMaxInactiveInterval(),
						TimeUnit.SECONDS, CACHE_NAMESPACE);
		} else if (cacheManager.supportsUpdateTtl()) {
			if (session.isDirty())
				cacheManager.put(session.getId(), sessionString, session.getMaxInactiveInterval(), TimeUnit.SECONDS,
						CACHE_NAMESPACE);
		} else {
			if (session.isDirty()
					|| session.getNow() - session.getLastAccessedTime() > session.getMinActiveInterval() * 1000)
				cacheManager.put(session.getId(), sessionString, session.getMaxInactiveInterval(), TimeUnit.SECONDS,
						CACHE_NAMESPACE);
		}
		if (maximumSessions > 0 && session.isDirty()) {
			try {
				kickoutOtherSession(session);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
	}

	public void kickoutOtherSession(WrappedHttpSession session) {
		String username = null;
		Object value = session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
		if (value != null) {
			Authentication auth = ((SecurityContext) value).getAuthentication();
			if (auth != null && auth.isAuthenticated()) {
				Object principal = auth.getPrincipal();
				if (principal instanceof UserDetails)
					username = ((UserDetails) principal).getUsername();
				else
					username = String.valueOf(principal);
			}
		}
		if (username != null) {
			String ip = session.getRequest().getRemoteAddr();
			String sessions = (String) cacheManager.get(username, CACHE_NAMESPACE);
			if (sessions == null) {
				sessions = session.getId();
			} else {
				List<String> list = new ArrayList<>();
				String[] arr = sessions.split(",");
				for (String id : arr) {
					String str = (String) cacheManager.get(id, CACHE_NAMESPACE);
					if (str != null && !str.contains(SESSION_KEY_KICKED_OUT_FROM))
						list.add(id);
				}
				if (!list.contains(session.getId()))
					list.add(session.getId());
				if (list.size() > maximumSessions) {
					for (int i = 0; i < list.size() - maximumSessions; i++) {
						String id = list.get(i);
						try {
							Map<String, String> map = new HashMap<>();
							map.put(SESSION_KEY_KICKED_OUT_FROM, ip);
							map.put(SESSION_KEY_KICKED_OUT_DATE, DateUtils.formatDatetime(new Date()));
							cacheManager.put(id, JsonUtils.toJson(map), session.getMaxInactiveInterval(),
									TimeUnit.SECONDS, CACHE_NAMESPACE);
							log.info("user[{}] session[{}] is kicked out by session[{}] from {}", username, id,
									session.getId(), ip);
						} catch (Exception e) {
							log.error(e.getMessage(), e);
						}
					}
					list = list.subList(list.size() - maximumSessions, list.size());
				}
				sessions = String.join(",", list);
			}
			cacheManager.put(username, sessions, 12, TimeUnit.HOURS, CACHE_NAMESPACE);
		}
	}

	@Override
	public void invalidate(WrappedHttpSession session) {
		cacheManager.delete(session.getId(), CACHE_NAMESPACE);
	}

}
