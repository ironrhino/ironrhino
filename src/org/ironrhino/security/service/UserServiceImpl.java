package org.ironrhino.security.service;

import java.util.List;

import org.ironrhino.core.spring.configuration.ClassPresentConditional;
import org.ironrhino.core.spring.security.ConcreteUserDetailsService;
import org.ironrhino.core.util.BeanUtils;
import org.ironrhino.security.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
@ClassPresentConditional("org.ironrhino.core.remoting.server.HttpInvokerServer")
public class UserServiceImpl implements UserService {

	@Autowired(required = false)
	private List<ConcreteUserDetailsService> userDetailsServices;

	@Override
	public boolean accepts(String username) {
		return true;
	}

	public UserDetails loadUserByUsername(String username) {
		if (username == null)
			throw new IllegalArgumentException("username shouldn't be null");
		UserDetails ud = null;
		if (userDetailsServices != null)
			for (ConcreteUserDetailsService uds : userDetailsServices) {
				if (uds instanceof UserService)
					continue;
				if (uds.accepts(username))
					try {
						ud = uds.loadUserByUsername(username);
						if (ud != null) {
							User user = new User();
							BeanUtils.copyProperties(ud, user);
							return user;
						}
					} catch (UsernameNotFoundException unfe) {
						continue;
					}
			}
		return null;
	}

}
