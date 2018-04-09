package org.ironrhino.core.security.event;

import org.ironrhino.core.spring.configuration.ResourcePresentConditional;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@ResourcePresentConditional("classpath*:resources/spring/applicationContext-security*.xml")
@Slf4j
public class SimpleLogoutEventListener {

	@EventListener
	public void onApplicationEvent(LogoutEvent event) {
		log.info(event.getUsername() + " logout from {}", event.getRemoteAddr());
	}

}
