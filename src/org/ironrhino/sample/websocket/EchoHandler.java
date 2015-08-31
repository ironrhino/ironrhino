package org.ironrhino.sample.websocket;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.AuthzUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class EchoHandler extends TextWebSocketHandler {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private UserDetailsService userDetailsService;

	private final Set<WebSocketSession> sessions = Collections
			.newSetFromMap(new ConcurrentHashMap<>());

	public void broadcast(String message, String... roles) {
		for (WebSocketSession s : sessions)
			if (s.isOpen()) {
				try {
					if (roles.length == 0 || AuthzUtils.authorizeUserDetails(
							userDetailsService.loadUserByUsername(s.getPrincipal().getName()), null,
							StringUtils.join(roles, ","), null))
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
			if (s.isOpen())
				try {
					if (p.test(userDetailsService.loadUserByUsername(s.getPrincipal().getName())))
						s.sendMessage(new TextMessage(message));
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				}
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		sessions.remove(session);
		super.afterConnectionClosed(session, status);
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		String username = session.getPrincipal().getName();
		if (username == null) {
			logger.warn("anonymous user denied");
			session.close(CloseStatus.NORMAL);
			return;
		} else {
			logger.info("connected with {}", username);
		}
		sessions.add(session);
		super.afterConnectionEstablished(session);
	}

	@Override
	public void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
		String username = session.getPrincipal().getName();
		logger.info("received \"{}\" from {}", message.getPayload(), username);
		String text = new StringBuilder(username).append(" send : ").append(message.getPayload()).toString();
		session.sendMessage(new TextMessage(text));
		broadcast("received \"" + message.getPayload() + "\" from " + username);
	}

}