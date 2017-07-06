package org.ironrhino.core.spring.security.password;

import org.springframework.security.crypto.password.PasswordEncoder;

public interface VersionedPasswordEncoder extends PasswordEncoder {

	public int getVersion();

}
