package org.ironrhino.security.event;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SimpleSignupEventListener {

	@EventListener
	public void onApplicationEvent(SignupEvent event) {
		log.info(event.getUsername() + " signup from {}"
				+ (event.getProvider() == null ? "" : ", via " + event.getProvider()), event.getRemoteAddr());
	}

}
