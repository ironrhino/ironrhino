package org.ironrhino.core.security.event;

import org.ironrhino.core.spring.configuration.ResourcePresentConditional;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@ResourcePresentConditional("classpath*:resources/spring/applicationContext-security*.xml")
@Slf4j
public class SimpleLoginEventListener {

	@EventListener
	public void onApplicationEvent(LoginEvent event) {
		log.info(event.getUsername() + " login from {}"
				+ (event.getProvider() == null ? "" : ", via " + event.getProvider()), event.getRemoteAddr());
	}

}
