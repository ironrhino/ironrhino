package org.ironrhino.core.websocket;

import java.util.Collections;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@EnableWebSocket
public abstract class AbstractWebSocketConfig implements WebSocketConfigurer {

	@Value("${cors.openForAllOrigin:false}")
	private boolean openForAllOrigin;

	@Autowired
	private Map<String, WebSocketHandler> webSocketHandlers = Collections.emptyMap();

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		webSocketHandlers.entrySet().stream().forEach(entry -> {
			String name = entry.getKey();
			if (name.endsWith("WebSocketHandler"))
				name = name.substring(0, name.length() - "WebSocketHandler".length());
			else if (name.endsWith("WebSocket"))
				name = name.substring(0, name.length() - "WebSocket".length());
			else if (name.endsWith("Handler"))
				name = name.substring(0, name.length() - "Handler".length());
			WebSocketHandlerRegistration registration = registry.addHandler(entry.getValue(), name);
			if (openForAllOrigin)
				registration.setAllowedOrigins("*");
		});
	}

}