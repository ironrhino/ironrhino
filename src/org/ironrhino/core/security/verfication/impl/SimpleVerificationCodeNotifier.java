package org.ironrhino.core.security.verfication.impl;

import org.ironrhino.core.security.verfication.VerificationCodeNotifier;
import org.ironrhino.core.spring.configuration.ApplicationContextPropertiesConditional;
import org.ironrhino.core.spring.configuration.StageConditional;
import org.ironrhino.core.util.AppInfo.Stage;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@StageConditional(Stage.DEVELOPMENT)
@ApplicationContextPropertiesConditional(key = "verification.code.enabled", value = "true")
@Component
public class SimpleVerificationCodeNotifier implements VerificationCodeNotifier {

	@Autowired
	private Logger logger;

	@Override
	public void send(String receiver, String code) {
		logger.info("send to {}: {}", receiver, code);
	}

}
