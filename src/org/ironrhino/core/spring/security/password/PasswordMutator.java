package org.ironrhino.core.spring.security.password;

import org.springframework.security.core.userdetails.UserDetails;

public interface PasswordMutator<T extends UserDetails> {

	boolean accepts(String username);

	void resetPassword(T user);

	default void removePassword(T user) {
		throw new UnsupportedOperationException();
	}

	void changePassword(T user, String password);

	void changePassword(T user, String currentPassword, String password);

}
