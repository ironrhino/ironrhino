package org.ironrhino.core.security.event;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SimplePasswordChangedEventListener {

	@EventListener
	public void onApplicationEvent(PasswordChangedEvent event) {
		log.info(event.getUsername() + " changed password from {}", event.getRemoteAddr());
	}

}
