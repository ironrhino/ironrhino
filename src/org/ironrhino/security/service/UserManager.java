package org.ironrhino.security.service;

import org.ironrhino.security.model.User;

public interface UserManager extends BaseUserManager<User> {

	public String suggestUsername(String candidate);

}
