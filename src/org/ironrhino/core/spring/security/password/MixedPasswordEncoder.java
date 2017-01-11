package org.ironrhino.core.spring.security.password;

import org.ironrhino.core.util.CodecUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

public class MixedPasswordEncoder implements PasswordEncoder {

	@Override
	public String encode(CharSequence rawPassword) {
		if (rawPassword == null)
			return null;
		return CodecUtils.digest(rawPassword.toString());
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