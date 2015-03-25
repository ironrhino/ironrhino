package org.ironrhino.security.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class SimpleLoginEventListener implements
		ApplicationListener<LoginEvent> {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public void onApplicationEvent(LoginEvent event) {
		logger.info(event.getUsername()
				+ " login"
				+ (event.getProvider() == null ? "" : ",via "
						+ event.getProvider()));
	}

}
