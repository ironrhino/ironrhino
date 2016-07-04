package org.ironrhino.security.event;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class SimpleSignupEventListener {

	@Autowired
	private Logger logger;

	@EventListener
	public void onApplicationEvent(SignupEvent event) {
		logger.info(event.getUsername() + " signup from {}"
				+ (event.getProvider() == null ? "" : ", via " + event.getProvider()), event.getRemoteAddr());
	}

}
