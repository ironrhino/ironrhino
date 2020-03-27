package org.ironrhino.core.spring.security.password;

import org.springframework.security.core.userdetails.UserDetails;

public interface PasswordMutator<T extends UserDetails> {

	boolean accepts(String username);

	void resetPassword(T user);

	void removePassword(T user);

	void changePassword(T user, String password);

	void changePassword(T user, String currentPassword, String password);

}
