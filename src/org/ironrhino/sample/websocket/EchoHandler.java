package org.ironrhino.sample.websocket;

import java.io.IOException;

import org.ironrhino.core.websocket.AuthorizedWebSocketHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Component
public class EchoHandler extends AuthorizedWebSocketHandler {

	@Override
	public void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
		String username = session.getPrincipal().getName();
		logger.info("received \"{}\" from {}", message.getPayload(), username);
		String text = new StringBuilder(username).append(" send : ").append(message.getPayload()).toString();
		session.sendMessage(new TextMessage(text));
		broadcast("received \"" + message.getPayload() + "\" from " + username);
	}

}