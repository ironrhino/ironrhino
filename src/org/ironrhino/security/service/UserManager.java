package org.ironrhino.security.service;

import org.ironrhino.core.service.BaseManager;
import org.ironrhino.core.spring.security.ConcreteUserDetailsService;
import org.ironrhino.security.model.User;

public interface UserManager extends BaseManager<User>,
		ConcreteUserDetailsService {

	public String suggestUsername(String candidate);

}
