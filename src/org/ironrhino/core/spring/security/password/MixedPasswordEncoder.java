package org.ironrhino.core.spring.security.password;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.CodecUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

public class MixedPasswordEncoder implements PasswordEncoder {

	@Override
	public String encode(CharSequence rawPassword) {
		if (rawPassword == null)
			return null;
		String input = rawPassword.toString();
		return input.length() == 40 && StringUtils.isAlphanumeric(input) ? CodecUtils.digestShaHex(input)
				: CodecUtils.digest(input);
	}

	@Override
	public boolean matches(CharSequence rawPassword, String encodedPassword) {
		if (rawPassword == null)
			rawPassword = "";
		if (encodedPassword == null)
			encodedPassword = "";
		rawPassword = encode(rawPassword);
		return encodedPassword.equals(rawPassword);
	}

}