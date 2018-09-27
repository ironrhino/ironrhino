package org.ironrhino.security.component;

import org.ironrhino.core.spring.configuration.StageConditional;
import org.ironrhino.core.spring.security.password.PasswordGenerator;
import org.ironrhino.core.spring.security.password.PasswordNotifier;
import org.ironrhino.core.util.AppInfo.Stage;
import org.ironrhino.security.model.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@StageConditional(Stage.DEVELOPMENT)
@Component
@Slf4j
public class SimplePasswordControl implements PasswordGenerator, PasswordNotifier {

	@Override
	public String generate(UserDetails user) {
		if (user instanceof User)
			return "password";
		return user.getUsername();
	}

	@Override
	public void notify(UserDetails user, String password) {
		log.info("send new password \"{}\" to {}", password, user.getUsername());
	}

}
