package org.ironrhino.core.spring.security;

import java.util.Comparator;
import java.util.List;

import javax.annotation.PostConstruct;

import org.ironrhino.core.spring.RemotingClientProxy;
import org.ironrhino.core.spring.configuration.ResourcePresentConditional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component("userDetailsService")
@Primary
@ResourcePresentConditional("classpath*:resources/spring/applicationContext-security*.xml")
public class DelegatedUserDetailsService implements UserDetailsService {

	@Autowired(required = false)
	private List<ConcreteUserDetailsService> userDetailsServices;

	@Autowired(required = false)
	private List<RemotingUserDetailsService> remotingUserDetailsServices;

	@PostConstruct
	private void init() {
		if (remotingUserDetailsServices != null) {
			remotingUserDetailsServices.sort(Comparator.comparing(ruds -> {
				for (Class<?> intf : ruds.getClass().getInterfaces()) {
					Order order = intf.getAnnotation(Order.class);
					if (order != null)
						return order.value();
				}
				return Ordered.LOWEST_PRECEDENCE;
			}));
		}
	}

	public UserDetails loadUserByUsername(String username, boolean nullInsteadException)
			throws UsernameNotFoundException {
		if (userDetailsServices != null) {
			for (ConcreteUserDetailsService uds : userDetailsServices)
				if (uds.accepts(username))
					try {
						UserDetails ud = uds.loadUserByUsername(username);
						if (ud != null)
							return ud;
					} catch (UsernameNotFoundException unfe) {
						continue;
					}
		}
		if (remotingUserDetailsServices != null) {
			for (RemotingUserDetailsService uds : remotingUserDetailsServices)
				if (uds instanceof RemotingClientProxy && uds.accepts(username)) {
					try {
						UserDetails ud = uds.loadUserByUsername(username);
						if (ud != null)
							return ud;
					} catch (UsernameNotFoundException unfe) {

					}
				}
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
