package org.ironrhino.core.spring.security.password;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.CodecUtils;

public class V1PasswordEncoder extends VersionedPasswordEncoder {

	@Override
	public String encode(CharSequence rawPassword) {
		if (rawPassword == null)
			return null;
		String input = rawPassword.toString();
		return input.length() == 40 && StringUtils.isAlphanumeric(input) ? CodecUtils.digestShaHex(input)
				: CodecUtils.digest(input);
	}

}
