package org.ironrhino.security.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class SimpleLoginEventListener {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@EventListener
	public void onApplicationEvent(LoginEvent event) {
		logger.info(event.getUsername() + " login from {}"
				+ (event.getProvider() == null ? "" : ", via " + event.getProvider()), event.getRemoteAddr());
	}

}
