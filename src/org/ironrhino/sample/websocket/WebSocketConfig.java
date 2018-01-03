package org.ironrhino.sample.websocket;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.spring.configuration.ClassPresentConditional;
import org.ironrhino.core.util.ReflectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@ComponentScan
@EnableWebSocket
@ClassPresentConditional(value = "com.caucho.config.Config", negated = true)
public class WebSocketConfig implements WebSocketConfigurer {

	@Autowired
	private List<WebSocketHandler> webSocketHandlers = Collections.emptyList();

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		webSocketHandlers.stream().forEach(handler -> {
			String name = StringUtils.uncapitalize(ReflectionUtils.getActualClass(handler).getSimpleName());
			if (name.endsWith("Handler"))
				name = name.substring(0, name.length() - 7);
			registry.addHandler(handler, name);
		});
	}

	@Bean
	public EchoHandler echoHandler() {
		return new EchoHandler();
	}

}