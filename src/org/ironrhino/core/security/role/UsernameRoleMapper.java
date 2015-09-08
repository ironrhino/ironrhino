package org.ironrhino.core.security.role;

import org.ironrhino.core.spring.configuration.ResourcePresentConditional;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
@ResourcePresentConditional("classpath*:resources/spring/applicationContext-security*.xml")
public class UsernameRoleMapper implements UserRoleMapper {

	@Override
	public String[] map(UserDetails user) {
		return new String[] { map(user.getUsername()) };
	}

	public static String map(String username) {
		return new StringBuilder("USERNAME(").append(username).append(")").toString();
	}

}
