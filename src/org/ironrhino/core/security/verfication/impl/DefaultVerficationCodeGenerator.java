package org.ironrhino.core.security.verfication.impl;

import org.ironrhino.core.security.verfication.VerficationCodeGenerator;
import org.ironrhino.core.spring.configuration.ApplicationContextPropertiesConditional;
import org.ironrhino.core.util.CodecUtils;
import org.springframework.stereotype.Component;

@ApplicationContextPropertiesConditional(key = "verification.code.enabled", value = "true")
@Component("verficationCodeGenerator")
public class DefaultVerficationCodeGenerator implements VerficationCodeGenerator {

	@Override
	public String generator(String receiver, int length) {
		return CodecUtils.randomDigitalString(length);
	}

}
