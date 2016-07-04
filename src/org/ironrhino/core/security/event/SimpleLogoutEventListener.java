package org.ironrhino.core.security.event;

import org.ironrhino.core.spring.configuration.ResourcePresentConditional;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@ResourcePresentConditional("classpath*:resources/spring/applicationContext-security*.xml")
public class SimpleLogoutEventListener {

	@Autowired
	private Logger logger;

	@EventListener
	public void onApplicationEvent(LogoutEvent event) {
		logger.info(event.getUsername() + " logout from {}", event.getRemoteAddr());
	}

}
