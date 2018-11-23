package org.ironrhino.security.service;

import org.hibernate.criterion.DetachedCriteria;
import org.ironrhino.core.hibernate.CriterionUtils;
import org.ironrhino.core.service.BaseManager;
import org.ironrhino.core.spring.security.ConcreteUserDetailsService;
import org.ironrhino.core.spring.security.password.PasswordMutator;
import org.ironrhino.security.model.BaseUser;

public interface BaseUserManager<T extends BaseUser>
		extends BaseManager<T>, PasswordMutator<T>, ConcreteUserDetailsService<T> {

	@Override
	default boolean accepts(String username) {
		return ConcreteUserDetailsService.super.accepts(username);
	}

	default DetachedCriteria detachedCriteria(String role) {
		DetachedCriteria dc = detachedCriteria();
		return dc.add(CriterionUtils.matchTag("roles", role));
	}

}
