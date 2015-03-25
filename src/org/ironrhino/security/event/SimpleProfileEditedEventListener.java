package org.ironrhino.security.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class SimpleProfileEditedEventListener implements
		ApplicationListener<ProfileEditedEvent> {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public void onApplicationEvent(ProfileEditedEvent event) {
		logger.info(event.getUsername() + " edited profile");
	}

}
