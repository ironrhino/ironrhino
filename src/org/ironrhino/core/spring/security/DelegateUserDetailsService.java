package org.ironrhino.core.spring.security;

import java.util.List;

import org.ironrhino.core.remoting.Remoting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
@Primary
@Remoting(UserDetailsService.class)
public class DelegateUserDetailsService implements UserDetailsService {

	@Autowired
	private List<ConcreteUserDetailsService> userDetailsServices;

	@Override
	public UserDetails loadUserByUsername(String username)
			throws UsernameNotFoundException {
		UserDetails ud = null;
		if (userDetailsServices != null)
			for (ConcreteUserDetailsService uds : userDetailsServices)
				try {
					ud = uds.loadUserByUsername(username);
					if (ud != null)
						return ud;
				} catch (UsernameNotFoundException unfe) {
					continue;
				}
		throw new UsernameNotFoundException("No such Username : " + username);
	}
}
