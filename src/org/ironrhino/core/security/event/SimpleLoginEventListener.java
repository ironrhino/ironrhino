package org.ironrhino.core.security.event;

import org.ironrhino.core.spring.security.SpringSecurityEnabled;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@SpringSecurityEnabled
@Slf4j
public class SimpleLoginEventListener {

	@EventListener
	public void onApplicationEvent(LoginEvent event) {
		log.info(event.getUsername() + " login from {}"
				+ (event.getProvider() == null ? "" : ", via " + event.getProvider()), event.getRemoteAddr());
	}

}
