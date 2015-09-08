package org.ironrhino.core.spring.security;

import java.util.List;

import org.ironrhino.core.remoting.Remoting;
import org.ironrhino.core.spring.configuration.ResourcePresentConditional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component("userDetailsService")
@Primary
@Remoting
@ResourcePresentConditional("classpath*:resources/spring/applicationContext-security*.xml")
public class DelegatedUserDetailsService implements UserDetailsService {

	@Autowired(required = false)
	private List<ConcreteUserDetailsService> userDetailsServices;

	public UserDetails loadUserByUsername(String username, boolean nullInsteadException)
			throws UsernameNotFoundException {
		UserDetails ud = null;
		if (userDetailsServices != null)
			for (ConcreteUserDetailsService uds : userDetailsServices)
				if (uds.accepts(username))
					try {
						ud = uds.loadUserByUsername(username);
						if (ud != null)
							return ud;
					} catch (UsernameNotFoundException unfe) {
						continue;
					}
		if (nullInsteadException)
			return null;
		throw new UsernameNotFoundException("No such Username : " + username);
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		return loadUserByUsername(username, false);
	}
}
