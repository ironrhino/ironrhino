package org.ironrhino.core.websocket;

import java.io.IOException;
import java.security.Principal;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.ironrhino.core.util.AuthzUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

public abstract class AuthorizedWebSocketHandler extends AbstractWebSocketHandler implements DisposableBean {

	protected Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private UserDetailsService userDetailsService;

	private final Set<WebSocketSession> sessions = Collections.newSetFromMap(new ConcurrentHashMap<>());

	protected boolean authorize(UserDetails user) {
		return true;
	}

	public void broadcast(String message, String... roles) {
		for (WebSocketSession s : sessions)
			if (s.isOpen()) {
				Principal principal = s.getPrincipal();
				if (principal == null)
					continue;
				try {
					if (roles.length == 0 || AuthzUtils.authorizeUserDetails(
							userDetailsService.loadUserByUsername(principal.getName()), null, roles, null))
						s.sendMessage(new TextMessage(message));
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				}
			} else {
				sessions.remove(s);
			}
	}

	public void broadcast(String message, Predicate<UserDetails> p) {
		for (WebSocketSession s : sessions)
			if (s.isOpen()) {
				Principal principal = s.getPrincipal();
				if (principal == null)
					continue;
				try {
					if (p.test(userDetailsService.loadUserByUsername(principal.getName())))
						s.sendMessage(new TextMessage(message));
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				}
			}
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		sessions.remove(session);
		super.afterConnectionClosed(session, status);
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		Principal principal = session.getPrincipal();
		String username = principal != null ? principal.getName() : null;
		if (username == null || !authorize(userDetailsService.loadUserByUsername(username))) {
			String message = "anonymous user denied";
			logger.warn(message);
			session.sendMessage(new TextMessage(message));
			session.close(CloseStatus.NORMAL);
			return;
		} else if (!authorize(userDetailsService.loadUserByUsername(username))) {
			String message = username + " denied";
			logger.warn(message);
			session.sendMessage(new TextMessage(message));
			session.close(CloseStatus.NORMAL);
			return;
		} else {
			logger.info("connected with {}", username);
		}
		sessions.add(session);
		super.afterConnectionEstablished(session);
	}

	@Override
	public void destroy() throws Exception {
		for (WebSocketSession session : sessions)
			if (session.isOpen())
				try {
					session.close(CloseStatus.NORMAL);
				} catch (IOException e) {
				}
	}

}