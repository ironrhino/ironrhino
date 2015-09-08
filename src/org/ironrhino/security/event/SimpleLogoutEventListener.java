package org.ironrhino.security.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class SimpleLogoutEventListener {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@EventListener
	public void onApplicationEvent(LogoutEvent event) {
		logger.info(event.getUsername() + " logout from {}", event.getRemoteAddr());
	}

}
