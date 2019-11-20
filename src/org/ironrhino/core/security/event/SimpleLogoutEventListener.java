package org.ironrhino.core.security.event;

import org.ironrhino.core.spring.security.SpringSecurityEnabled;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@SpringSecurityEnabled
@Slf4j
public class SimpleLogoutEventListener {

	@EventListener
	public void onApplicationEvent(LogoutEvent event) {
		log.info(event.getUsername() + " logout from {}", event.getRemoteAddr());
	}

}
