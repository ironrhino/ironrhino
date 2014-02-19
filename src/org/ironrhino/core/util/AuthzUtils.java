package org.ironrhino.core.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.model.Secured;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

public class AuthzUtils {

	public static Object authentication(String property) {
		Authentication auth = SecurityContextHolder.getContext()
				.getAuthentication();
		if (auth.getPrincipal() == null)
			return null;
		try {
			BeanWrapperImpl wrapper = new BeanWrapperImpl(auth);
			return wrapper.getPropertyValue(property);
		} catch (BeansException e) {
			return null;
		}
	}

	public static boolean authorize(String ifAllGranted, String ifAnyGranted,
			String ifNotGranted) {
		return authorizeRoles(getRoleNames(), ifAllGranted, ifAnyGranted,
				ifNotGranted);
	}

	public static boolean authorizeUserDetails(UserDetails user,
			String ifAllGranted, String ifAnyGranted, String ifNotGranted) {
		return authorizeRoles(getRoleNamesFromUserDetails(user), ifAllGranted,
				ifAnyGranted, ifNotGranted);
	}

	public static boolean authorizeRoles(List<String> roles,
			String ifAllGranted, String ifAnyGranted, String ifNotGranted) {
		if (StringUtils.isNotBlank(ifAllGranted)) {
			String[] arr = ifAllGranted.split("\\s*,\\s*");
			for (String s : arr)
				if (!roles.contains(s.trim()))
					return false;
			return true;
		} else if (StringUtils.isNotBlank(ifAnyGranted)) {
			String[] arr = ifAnyGranted.split("\\s*,\\s*");
			for (String s : arr)
				if (roles.contains(s.trim()))
					return true;
			return false;
		} else if (StringUtils.isNotBlank(ifNotGranted)) {
			String[] arr = ifNotGranted.split("\\s*,\\s*");
			boolean b = true;
			for (String s : arr)
				if (roles.contains(s.trim())) {
					b = false;
					break;
				}
			return b;
		}
		return false;
	}

	public static List<String> getRoleNames() {
		List<String> roleNames = new ArrayList<String>();
		if (SecurityContextHolder.getContext().getAuthentication() != null) {
			Collection<? extends GrantedAuthority> authz = SecurityContextHolder
					.getContext().getAuthentication().getAuthorities();
			if (authz != null)
				for (GrantedAuthority var : authz)
					roleNames.add(var.getAuthority());
		}
		return roleNames;
	}

	public static List<String> getRoleNamesFromUserDetails(UserDetails user) {
		List<String> roleNames = new ArrayList<String>();
		Collection<? extends GrantedAuthority> authz = user.getAuthorities();
		if (authz != null)
			for (GrantedAuthority var : authz)
				roleNames.add(var.getAuthority());
		return roleNames;
	}

	public static boolean hasPermission(Secured entity) {
		return hasPermission(entity, false);
	}

	public static boolean hasPermission(Secured entity, boolean defaultWhenEmpty) {
		if (entity == null)
			return false;
		if (entity.getRoles() == null || entity.getRoles().size() == 0)
			return defaultWhenEmpty;
		List<String> roleNames = getRoleNames();
		for (String s : entity.getRoles()) {
			if (roleNames.contains(s))
				return true;
		}
		return false;
	}

	public static String getUsername() {
		SecurityContext sc = SecurityContextHolder.getContext();
		if (sc == null)
			return null;
		Authentication auth = sc.getAuthentication();
		if (auth == null)
			return null;
		return auth.getName();
	}

	@SuppressWarnings("unchecked")
	public static <T extends UserDetails> T getUserDetails() {
		SecurityContext sc = SecurityContextHolder.getContext();
		if (sc != null) {
			Authentication auth = sc.getAuthentication();
			if (auth != null) {
				Object principal = auth.getPrincipal();
				return principal instanceof UserDetails ? (T) principal : null;
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static <T extends UserDetails> T getUserDetails(Class<T> clazz) {
		UserDetails ud = getUserDetails();
		return ud != null && clazz.isAssignableFrom(ud.getClass()) ? (T) ud
				: null;
	}

	public static void autoLogin(UserDetails ud) {
		SecurityContext sc = SecurityContextHolder.getContext();
		Authentication auth = new UsernamePasswordAuthenticationToken(ud,
				ud.getPassword(), ud.getAuthorities());
		sc.setAuthentication(auth);
	}

	public static String encodePassword(UserDetails ud, String input) {
		PasswordEncoder encoder = ApplicationContextUtils
				.getBean(PasswordEncoder.class);
		return encoder.encode(input);
	}

	public static boolean isPasswordValid(UserDetails ud, String password) {
		PasswordEncoder encoder = ApplicationContextUtils
				.getBean(PasswordEncoder.class);
		return encoder.matches(password, ud.getPassword());
	}

	public static boolean isPasswordValid(String password) {
		Object principal = SecurityContextHolder.getContext()
				.getAuthentication().getPrincipal();
		return principal instanceof UserDetails ? isPasswordValid(
				(UserDetails) principal, password) : false;
	}
}