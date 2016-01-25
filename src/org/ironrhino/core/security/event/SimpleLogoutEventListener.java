package org.ironrhino.core.security.event;

import org.ironrhino.core.spring.configuration.ResourcePresentConditional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@ResourcePresentConditional("classpath*:resources/spring/applicationContext-security*.xml")
public class SimpleLogoutEventListener {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@EventListener
	public void onApplicationEvent(LogoutEvent event) {
		logger.info(event.getUsername() + " logout from {}", event.getRemoteAddr());
	}

}
