package org.ironrhino.core.spring.security.password;

import java.util.Scanner;
import java.util.regex.MatchResult;

import org.springframework.security.crypto.password.PasswordEncoder;

public abstract class VersionedPasswordEncoder implements PasswordEncoder {

	protected int version;

	public int getVersion() {
		if (version == 0) {
			String classname = getClass().getSimpleName();
			Scanner s = new Scanner(classname);
			s.findInLine("\\D*(\\d+)\\D*");
			try {
				MatchResult result = s.match();
				version = Integer.valueOf(result.group(1));
				if (version < 1)
					version = 1;
			} catch (Exception e) {
				version = 1;
			}
			s.close();
		}
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
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
