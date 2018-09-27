package org.ironrhino.core.spring.security.password;

import org.springframework.security.core.userdetails.UserDetails;

public interface PasswordNotifier {

	void notify(UserDetails user, String password);

}
