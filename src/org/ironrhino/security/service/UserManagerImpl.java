package org.ironrhino.security.service;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.cache.CheckCache;
import org.ironrhino.core.cache.EvictCache;
import org.ironrhino.core.remoting.Remoting;
import org.ironrhino.core.security.role.UserRole;
import org.ironrhino.core.security.role.UserRoleMapper;
import org.ironrhino.core.service.BaseManagerImpl;
import org.ironrhino.core.spring.security.FallbackUserDetailsService;
import org.ironrhino.core.util.CodecUtils;
import org.ironrhino.security.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Primary
@Remoting(UserDetailsService.class)
public class UserManagerImpl extends BaseManagerImpl<User> implements
		UserManager {

	@Autowired(required = false)
	private List<UserRoleMapper> userRoleMappers;

	@Autowired(required = false)
	private FallbackUserDetailsService fallbackUserDetailsService;

	@Override
	@Transactional
	@EvictCache(namespace = "user", key = "${[user.username,user.email]}")
	public void delete(User user) {
		super.delete(user);
	}

	@Override
	@Transactional
	@EvictCache(namespace = "user", key = "${[user.username,user.email]}")
	public void save(User user) {
		super.save(user);
	}

	@Override
	@Transactional
	@EvictCache(namespace = "user", key = "${key = [];foreach (user : retval) { key.add(user.username); key.add(user.email);} return key;}")
	public List<User> delete(Serializable... id) {
		return super.delete(id);
	}

	@Override
	@Transactional(readOnly = true)
	@CheckCache(namespace = "user", key = "${username}")
	public UserDetails loadUserByUsername(String username) {
		if (StringUtils.isBlank(username))
			throw new UsernameNotFoundException("username is blank");
		username = username.toLowerCase();
		User user;
		if (username.indexOf('@') > 0)
			user = findOne("email", username);
		else
			user = findByNaturalId(username);
		if (user == null && fallbackUserDetailsService != null)
			return fallbackUserDetailsService.loadUserByUsername(username);
		if (user == null)
			throw new UsernameNotFoundException("No such Username : "
					+ username);
		populateAuthorities(user);
		return user;
	}

	private void populateAuthorities(User user) {
		List<GrantedAuthority> auths = new ArrayList<GrantedAuthority>();
		auths.add(new SimpleGrantedAuthority(UserRole.ROLE_BUILTIN_USER));
		for (String role : user.getRoles())
			auths.add(new SimpleGrantedAuthority(role));
		if (userRoleMappers != null)
			for (UserRoleMapper mapper : userRoleMappers) {
				String[] roles = mapper.map(user);
				if (roles != null)
					for (String role : roles)
						auths.add(new SimpleGrantedAuthority(role));
			}
		user.setAuthorities(auths);
	}

	@Override
	public String suggestUsername(String candidate) {
		if (candidate.indexOf("://") > 0) {
			try {
				URL url = new URL(candidate);
				String path = url.getPath();
				if (path.length() > 1) {
					candidate = path.substring(1);
					if (candidate.endsWith("/"))
						candidate = candidate.substring(0,
								candidate.length() - 1);
				} else {
					candidate = candidate
							.substring(candidate.indexOf("://") + 3);
					String temp = candidate
							.substring(0, candidate.indexOf('.'));
					if (!temp.equalsIgnoreCase("www")) {
						candidate = temp;
					} else {
						candidate = candidate
								.substring(candidate.indexOf('.') + 1);
						candidate = candidate.substring(0,
								candidate.indexOf('.'));
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

}
