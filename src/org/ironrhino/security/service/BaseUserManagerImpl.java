package org.ironrhino.security.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.security.role.UserRoleMapper;
import org.ironrhino.core.service.BaseManagerImpl;
import org.ironrhino.core.spring.security.password.PasswordGenerator;
import org.ironrhino.core.spring.security.password.PasswordNotifier;
import org.ironrhino.security.model.BaseUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public abstract class BaseUserManagerImpl<T extends BaseUser> extends BaseManagerImpl<T> implements BaseUserManager<T> {

	@Autowired(required = false)
	private PasswordGenerator passwordGenerator;

	@Autowired(required = false)
	private PasswordNotifier passwordNotifier;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired(required = false)
	private List<UserRoleMapper> userRoleMappers;

	@Value("${user.password.expiresInDays:0}")
	private int passwordExpiresInDays;

	@Override
	@Transactional
	public void save(T user) {
		if (user.isNew() && user.getPassword() == null) {
			resetPassword(user);
		}
		super.save(user);
	}

	@Transactional
	@Override
	public void resetPassword(T user) {
		T u = get(user.getId());
		String newPassword = passwordGenerator != null ? passwordGenerator.generate(user) : user.getUsername();
		if (newPassword == null)
			newPassword = user.getUsername();
		String password = newPassword;
		u.setPassword(passwordEncoder.encode(password));
		u.setPasswordModifyDate(new Date(0)); // magic date for user.isCredentialsNonExpired()
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				user.setPassword(u.getPassword());
				user.setPasswordModifyDate(u.getPasswordModifyDate());
				if (passwordNotifier != null)
					passwordNotifier.notify(u, password);
			}
		});
		super.save(u);
	}

	@Transactional
	@Override
	public void changePassword(T user, String password) {
		T u = get(user.getId());
		u.setPassword(passwordEncoder.encode(password));
		u.setPasswordModifyDate(new Date());
		// copy state to origin object to avoid re-login
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				user.setPassword(u.getPassword());
				user.setPasswordModifyDate(u.getPasswordModifyDate());
			}
		});
		super.save(u);
	}

	@Override
	@Transactional(readOnly = true)
	public T loadUserByUsername(String username) {
		if (StringUtils.isBlank(username))
			return null;
		T user = doLoadUserByUsername(username);
		if (user == null) {
			// throw new UsernameNotFoundException("No such Username : "+
			// username);
			return null; // for @CheckCache
		}
		populateAuthorities(user);
		populateExpires(user);
		return user;
	}

	protected T doLoadUserByUsername(String username) {
		return findByNaturalId(username);
	}

	protected void populateAuthorities(T user) {
		List<GrantedAuthority> auths = new ArrayList<>();
		Set<String> set = getBuiltInRoles();
		auths.addAll(AuthorityUtils.createAuthorityList(set.toArray(new String[set.size()])));
		set = user.getRoles();
		auths.addAll(AuthorityUtils.createAuthorityList(set.toArray(new String[set.size()])));
		user.setAuthorities(auths);
		if (userRoleMappers != null)
			for (UserRoleMapper mapper : userRoleMappers) {
				String[] roles = mapper.map(user);
				if (roles != null)
					for (String role : roles)
						auths.add(new SimpleGrantedAuthority(role));
			}
	}

	protected void populateExpires(T user) {
		if (passwordExpiresInDays > 0)
			user.setPasswordExpiresInDays(passwordExpiresInDays);
	}

	protected Set<String> getBuiltInRoles() {
		return Collections.emptySet();
	}

}
