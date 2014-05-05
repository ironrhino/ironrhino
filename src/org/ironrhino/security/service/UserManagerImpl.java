package org.ironrhino.security.service;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.hibernate.criterion.DetachedCriteria;
import org.ironrhino.core.cache.CheckCache;
import org.ironrhino.core.cache.EvictCache;
import org.ironrhino.core.hibernate.CriterionUtils;
import org.ironrhino.core.security.role.UserRole;
import org.ironrhino.core.security.role.UserRoleMapper;
import org.ironrhino.core.service.BaseManagerImpl;
import org.ironrhino.core.util.CodecUtils;
import org.ironrhino.security.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户接口实现，通过范型重载基类实现
 */
@Component
@Order(0)
public class UserManagerImpl extends BaseManagerImpl<User> implements
		UserManager {

	@Autowired(required = false)
	private List<UserRoleMapper> userRoleMappers;

	// 密码过期天数
	@Value("${userManager.passwordExpiresInDays:0}")
	private int passwordExpiresInDays;

	/**
	 * 删除用户成功后AOP实现清空缓存中的用户对象
	 */
	@Override
	@Transactional
	@EvictCache(namespace = "user", key = "${[user.username,user.email]}")
	public void delete(User user) {
		super.delete(user);
	}

	/**
     * 新增用户成功后AOP实现清空缓存中的用户对象
     */
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
	public boolean accepts(String username) {
		return true;
	}

	@Override
	@Transactional(readOnly = true)
	@CheckCache(namespace = "user", key = "${username}")
	public UserDetails loadUserByUsername(String username) {
		if (StringUtils.isBlank(username))
			return null;
		// throw new UsernameNotFoundException("username is blank");
		username = username.toLowerCase();
		User user;
		if (username.indexOf('@') > 0)
			user = findOne("email", username);
		else
			user = findByNaturalId(username);
		if (user == null) {
			// throw new UsernameNotFoundException("No such Username : "+
			// username);
			return null; // for @CheckCache
		}
		if (passwordExpiresInDays > 0) {
			Date passwordModifyDate = user.getPasswordModifyDate();
			if (passwordModifyDate != null
					&& DateUtils.addDays(passwordModifyDate,
							passwordExpiresInDays).before(new Date()))
				user.setPasswordExpired(true);
		}
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

	public DetachedCriteria detachedCriteria(String role) {
		DetachedCriteria dc = detachedCriteria();
		return dc.add(CriterionUtils.matchTag("rolesAsString", role));
	}

}
