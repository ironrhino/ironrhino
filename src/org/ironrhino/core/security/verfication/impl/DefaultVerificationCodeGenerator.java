package org.ironrhino.core.security.verfication.impl;

import org.ironrhino.core.security.verfication.VerificationCodeGenerator;
import org.ironrhino.core.spring.configuration.ApplicationContextPropertiesConditional;
import org.ironrhino.core.util.CodecUtils;
import org.springframework.stereotype.Component;

@ApplicationContextPropertiesConditional(key = "verification.code.enabled", value = "true")
@Component("verificationCodeGenerator")
public class DefaultVerificationCodeGenerator implements VerificationCodeGenerator {

	@Override
	public String generator(String receiver, int length) {
		return CodecUtils.randomDigitalString(length);
	}

}
