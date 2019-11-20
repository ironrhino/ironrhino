package org.ironrhino.core.security.verfication.impl;

import org.ironrhino.core.security.verfication.VerificationCodeEnabled;
import org.ironrhino.core.security.verfication.VerificationCodeNotifier;
import org.ironrhino.core.spring.configuration.StageConditional;
import org.ironrhino.core.util.AppInfo.Stage;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@StageConditional(Stage.DEVELOPMENT)
@VerificationCodeEnabled
@Component
@Slf4j
public class SimpleVerificationCodeNotifier implements VerificationCodeNotifier {

	@Override
	public void send(String receiver, String code) {
		log.info("send to {}: {}", receiver, code);
	}

}
