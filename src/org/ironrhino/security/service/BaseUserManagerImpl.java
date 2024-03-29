package org.ironrhino.security.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.aop.AopContext;
import org.ironrhino.core.cache.CacheNamespaceProvider;
import org.ironrhino.core.cache.CheckCache;
import org.ironrhino.core.cache.EvictCache;
import org.ironrhino.core.security.role.UserRoleMapper;
import org.ironrhino.core.service.BaseManagerImpl;
import org.ironrhino.core.spring.security.password.PasswordGenerator;
import org.ironrhino.core.spring.security.password.PasswordNotifier;
import org.ironrhino.core.spring.security.password.PasswordUsedException;
import org.ironrhino.security.model.BaseUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import lombok.Getter;

public abstract class BaseUserManagerImpl<T extends BaseUser> extends BaseManagerImpl<T>
		implements BaseUserManager<T>, CacheNamespaceProvider {

	protected static final String DEFAULT_CACHE_NAMESPACE = "user";

	@Getter
	protected String cacheNamespace = DEFAULT_CACHE_NAMESPACE;

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

	@Value("${user.password.maxRemembered:0}")
	private int maxRememberedPasswords;

	@Value("${user.cache.namespace:" + DEFAULT_CACHE_NAMESPACE + "}")
	public void setCacheNamespace(String cacheNamespace) {
		this.cacheNamespace = cacheNamespace;
	}

	@Override
	@Transactional
	@EvictCache(key = "${user.username}")
	public void save(T user) {
		if (user.isNew() && user.getPassword() == null) {
			resetPassword(user);
		}
		super.save(user);
	}

	@Override
	@Transactional
	@EvictCache(key = "${user.username}", renew = "${user}")
	public void update(T user) {
		super.update(user);

		// for renew
		if (user.getAuthorities().isEmpty()) {
			populateAuthorities(user);
			populateExpires(user);
		}
	}

	@Override
	@Transactional
	@EvictCache(key = "${user.username}")
	public void delete(T user) {
		super.delete(user);
	}

	@Override
	@Transactional
	@EvictCache(key = "${key = [];foreach (user : " + AopContext.CONTEXT_KEY_RETVAL
			+ ") { key.add(user.username); } return key;}")
	public List<T> delete(Serializable... id) {
		return super.delete(id);
	}

	@Override
	@Transactional
	@EvictCache(key = "${user.username}")
	public void resetPassword(T user) {
		T u = user.isNew() ? user : get(user.getId());
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

	@Override
	@Transactional
	@EvictCache(key = "${user.username}")
	public void removePassword(T user) {
		T u = user.isNew() ? user : get(user.getId());
		u.setPassword("");
		u.setPasswordModifyDate(null);
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				user.setPassword("");
				u.setPasswordModifyDate(null);
			}
		});
		super.save(u);
	}

	@Override
	@Transactional
	@EvictCache(key = "${user.username}")
	public void changePassword(T user, String password) {
		T u = get(user.getId());
		checkUsedPassword(u, password);
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
	@Transactional
	@EvictCache(key = "${user.username}")
	public void changePassword(T user, String currentPassword, String password) {
		T u = get(user.getId());
		if (!passwordEncoder.matches(currentPassword, u.getPassword()))
			throw new BadCredentialsException("Bad credentials");
		checkUsedPassword(u, password);
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
	@Transactional
	@EvictCache(key = "${user.username}")
	public T updatePassword(UserDetails user, String encodedPassword) {
		T u = doLoadUserByUsername(user.getUsername());
		u.setPassword(encodedPassword);
		u.setPasswordModifyDate(new Date());
		// copy state to origin object to avoid re-login
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				@SuppressWarnings("unchecked")
				T u2 = (T) user;
				u2.setPassword(u.getPassword());
				u2.setPasswordModifyDate(u.getPasswordModifyDate());
			}
		});
		super.save(u);
		return u;
	}

	@Override
	@Transactional(readOnly = true)
	@CheckCache(key = "${username}", cacheNull = true)
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
		return findByNaturalId(username.toLowerCase(Locale.ROOT));
	}

	protected void populateAuthorities(T user) {
		List<GrantedAuthority> auths = new ArrayList<>();
		Set<String> set = getBuiltInRoles();
		auths.addAll(AuthorityUtils.createAuthorityList(set.toArray(new String[0])));
		set = user.getRoles();
		auths.addAll(AuthorityUtils.createAuthorityList(set.toArray(new String[0])));
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

	protected void checkUsedPassword(T user, String password) {
		List<String> rememberedPasswords = new ArrayList<>();
		if (user.getRememberedPasswords() != null)
			rememberedPasswords.addAll(user.getRememberedPasswords());
		rememberedPasswords.add(user.getPassword());
		for (String pw : rememberedPasswords) {
			if (passwordEncoder.matches(password, pw))
				throw new PasswordUsedException("Password recently used");
		}
		int tobeRemoved = rememberedPasswords.size() - maxRememberedPasswords;
		if (tobeRemoved > 0) {
			for (int i = 0; i < tobeRemoved; i++)
				rememberedPasswords.remove(0);
			if (rememberedPasswords.isEmpty())
				rememberedPasswords = null;
		}
		user.setRememberedPasswords(rememberedPasswords);
	}

}
