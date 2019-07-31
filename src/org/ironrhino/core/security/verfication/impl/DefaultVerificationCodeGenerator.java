package org.ironrhino.core.security.verfication.impl;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.security.verfication.VerificationCodeGenerator;
import org.ironrhino.core.spring.configuration.ApplicationContextPropertiesConditional;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.AppInfo.Stage;
import org.ironrhino.core.util.CodecUtils;
import org.springframework.stereotype.Component;

@ApplicationContextPropertiesConditional(key = "verification.code.enabled", value = "true")
@Component("verificationCodeGenerator")
public class DefaultVerificationCodeGenerator implements VerificationCodeGenerator {

	@Override
	public String generator(String receiver, int length) {
		if (AppInfo.getStage() == Stage.DEVELOPMENT)
			return StringUtils.repeat('0', length);
		return CodecUtils.randomDigitalString(length);
	}

}
