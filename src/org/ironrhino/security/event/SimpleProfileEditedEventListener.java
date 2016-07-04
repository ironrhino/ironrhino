package org.ironrhino.security.event;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class SimpleProfileEditedEventListener {

	@Autowired
	private Logger logger;

	@EventListener
	public void onApplicationEvent(ProfileEditedEvent event) {
		logger.info(event.getUsername() + " edited profile from {}", event.getRemoteAddr());
	}

}
