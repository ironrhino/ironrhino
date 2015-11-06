package org.ironrhino.sample.remoting.service;

import org.ironrhino.sample.remoting.domain.User;
import org.ironrhino.security.service.UserManager;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UserServiceImpl implements UserService {

	@Autowired
	private UserManager userManager;

	@Override
	public boolean accepts(String username) {
		return true;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String username) {
		UserDetails u = userManager.loadUserByUsername(username);
		if (u == null)
			return null;
		User user = new User();
		BeanUtils.copyProperties(u, user);
		return user;
	}

}
