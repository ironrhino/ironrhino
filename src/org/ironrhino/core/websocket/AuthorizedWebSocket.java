package org.ironrhino.core.websocket;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PreDestroy;
import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.Session;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.AuthzUtils;
import org.ironrhino.core.util.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

public class AuthorizedWebSocket {

	public static final String USER_PROPERTIES_NAME_USERNAME = "username";

	protected Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private UserDetailsService userDetailsService;

	private final Set<Session> sessions = Collections
			.newSetFromMap(new ConcurrentHashMap<Session, Boolean>());

	protected boolean authorize(UserDetails user) {
		return true;
	}

	public void broadcast(String message, String... roles) {
		for (Session s : sessions)
			if (s.isOpen()) {
				try {
					if (roles.length == 0
							|| AuthzUtils
									.authorizeUserDetails(
											userDetailsService
													.loadUserByUsername((String) s
															.getUserProperties()
															.get(USER_PROPERTIES_NAME_USERNAME)),
											null, StringUtils.join(roles, ","),
											null))
						s.getBasicRemote().sendText(message);
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				}
			} else {
				sessions.remove(s);
			}
	}

	public void broadcast(String message, Predicate<UserDetails> p) {
		for (Session s : sessions)
			if (s.isOpen())
				try {
					if (p.evaluate(userDetailsService
							.loadUserByUsername((String) s.getUserProperties()
									.get(USER_PROPERTIES_NAME_USERNAME))))
						s.getBasicRemote().sendText(message);
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				}
	}

	public void onMessage(Session session, String message) {
		logger.info("received from {} : {}",
				session.getUserProperties().get(USER_PROPERTIES_NAME_USERNAME),
				message);
	}

	public void onOpen(Session session) {
		UserDetails user = null;
		Object principal = session.getUserPrincipal();
		if (principal instanceof UsernamePasswordAuthenticationToken) {
			UsernamePasswordAuthenticationToken upat = (UsernamePasswordAuthenticationToken) principal;
			principal = upat.getPrincipal();
			if (principal instanceof UserDetails)
				user = (UserDetails) principal;
		}
		if (user == null || !authorize(user)) {
			try {
				session.getBasicRemote().sendText("access.denied");
				session.close(new CloseReason(CloseCodes.NORMAL_CLOSURE,
						"access.denied"));
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		} else {
			session.getUserProperties().put(USER_PROPERTIES_NAME_USERNAME,
					user.getUsername());
			sessions.add(session);
		}
	}

	public void onClose(Session session) {
		sessions.remove(session);
	}

	public void onError(Session session, Throwable err) {
		if (err instanceof IOException) {
			if (logger.isDebugEnabled())
				logger.debug(err.getMessage(), err);
			sessions.remove(session);
			if (session.isOpen())
				try {
					session.close(new CloseReason(CloseCodes.CLOSED_ABNORMALLY,
							err.getMessage()));
				} catch (IllegalStateException | IOException e) {
				}
		} else {
			logger.error(err.getMessage(), err);
		}

	}

	@PreDestroy
	public void destroy() {
		for (Session s : sessions) {
			if (s.isOpen())
				try {
					s.close(new CloseReason(CloseCodes.NORMAL_CLOSURE, ""));
				} catch (IllegalStateException | IOException e) {
				}
		}
	}
}
