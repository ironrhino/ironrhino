package org.ironrhino.core.spring.security.password;

import org.ironrhino.core.util.CodecUtils;

public class V1PasswordEncoder extends VersionedPasswordEncoder {

	@Override
	public String encode(CharSequence rawPassword) {
		if (rawPassword == null)
			return null;
		return CodecUtils.digest(rawPassword.toString());
	}

}
