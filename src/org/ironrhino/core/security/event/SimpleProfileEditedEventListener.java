package org.ironrhino.core.security.event;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SimpleProfileEditedEventListener {

	@EventListener
	public void onApplicationEvent(ProfileEditedEvent event) {
		log.info(event.getUsername() + " edited profile from {}", event.getRemoteAddr());
	}

}
