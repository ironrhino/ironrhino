package org.ironrhino.security.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class SimpleSignupEventListener implements
		ApplicationListener<SignupEvent> {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public void onApplicationEvent(SignupEvent event) {
		logger.info(
				event.getUsername()
						+ " signup from {}"
						+ (event.getProvider() == null ? "" : ", via "
								+ event.getProvider()), event.getRemoteAddr());
	}

}
