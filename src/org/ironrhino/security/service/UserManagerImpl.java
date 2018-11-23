package org.ironrhino.security.service;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.ironrhino.core.aop.AopContext;
import org.ironrhino.core.cache.CheckCache;
import org.ironrhino.core.cache.EvictCache;
import org.ironrhino.core.security.role.UserRole;
import org.ironrhino.core.util.CodecUtils;
import org.ironrhino.security.model.User;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Order(0)
public class UserManagerImpl extends BaseUserManagerImpl<User> implements UserManager {

	private static final String CACHE_NAMESPACE = "user";

	@Override
	@Transactional
	@EvictCache(namespace = CACHE_NAMESPACE, key = "${[user.username,user.email]}")
	public void delete(User user) {
		super.delete(user);
	}

	@Override
	@Transactional
	@EvictCache(namespace = CACHE_NAMESPACE, key = "${[user.username,user.email]}")
	public void save(User user) {
		super.save(user);
	}

	@Override
	@Transactional
	@EvictCache(namespace = CACHE_NAMESPACE, key = "${[user.username,user.email]}")
	public void update(User user) {
		super.update(user);
	}

	@Override
	@Transactional
	@EvictCache(namespace = CACHE_NAMESPACE, key = "${key = [];foreach (user : " + AopContext.CONTEXT_KEY_RETVAL
			+ ") { key.add(user.username); key.add(user.email);} return key;}")
	public List<User> delete(Serializable... id) {
		return super.delete(id);
	}

	@Override
	@Transactional
	@EvictCache(namespace = CACHE_NAMESPACE, key = "${[user.username,user.email]}")
	public void resetPassword(User user) {
		super.resetPassword(user);
	}

	@Override
	@Transactional
	@EvictCache(namespace = CACHE_NAMESPACE, key = "${[user.username,user.email]}")
	public void changePassword(User user, String password) {
		super.changePassword(user, password);
	}

	@Override
	@Transactional(readOnly = true)
	@CheckCache(namespace = CACHE_NAMESPACE, key = "${username}", cacheNull = true)
	public User loadUserByUsername(String username) {
		return super.loadUserByUsername(username);
	}

	@Override
	protected User doLoadUserByUsername(String username) {
		username = username.toLowerCase(Locale.ROOT);
		User user;
		if (username.indexOf('@') > 0)
			user = findOne("email", username);
		else
			user = findByNaturalId(username);
		return user;
	}

	@Override
	@Transactional(readOnly = true)
	public String suggestUsername(String candidate) {
		if (candidate.indexOf("://") > 0) {
			try {
				URL url = new URL(candidate);
				String path = url.getPath();
				if (path.length() > 1) {
					candidate = path.substring(1);
					if (candidate.endsWith("/"))
						candidate = candidate.substring(0, candidate.length() - 1);
				} else {
					candidate = candidate.substring(candidate.indexOf("://") + 3);
					String temp = candidate.substring(0, candidate.indexOf('.'));
					if (!temp.equalsIgnoreCase("www")) {
						candidate = temp;
					} else {
						candidate = candidate.substring(candidate.indexOf('.') + 1);
						candidate = candidate.substring(0, candidate.indexOf('.'));
					}
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
		int i = candidate.indexOf('@');
		if (i > 0)
			candidate = candidate.substring(0, i);
		candidate = candidate.replace('.', '_');
		User user = findByNaturalId(candidate);
		if (user == null)
			return candidate;
		i = 10;
		int digits = 1;
		i = CodecUtils.randomInt(digits);
		user = findByNaturalId(candidate + i);
		while (user != null) {
			digits++;
			i = CodecUtils.randomInt(digits);
			user = findByNaturalId(candidate + i);
		}
		return candidate + i;
	}

	@Override
	protected Set<String> getBuiltInRoles() {
		return Collections.singleton(UserRole.ROLE_BUILTIN_USER);
	}

}
