package org.ironrhino.core.security.dynauth;

import java.util.List;

import org.ironrhino.core.spring.security.SpringSecurityEnabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@SpringSecurityEnabled
@Slf4j
public class DynamicAuthorizerManager {

	@Autowired(required = false)
	private List<DynamicAuthorizer> authorizers;

	public boolean authorize(Class<?> authorizer, UserDetails user, String resource) {
		return authorize(authorizer.getName(), user, resource);
	}

	public boolean authorize(String authorizer, UserDetails user, String resource) {
		if (authorizers != null) {
			for (DynamicAuthorizer entry : authorizers) {
				if (entry.getClass().getName().equals(authorizer))
					return entry.authorize(user, resource);
			}
			log.error("not found authorizer [{}] in spring applicationContext", authorizer);
		}
		return false;
	}

}
