package org.ironrhino.security.service;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Set;

import org.ironrhino.core.security.role.UserRole;
import org.ironrhino.core.util.CodecUtils;
import org.ironrhino.security.model.User;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Order(0)
public class UserManagerImpl extends BaseUserManagerImpl<User> implements UserManager {

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
		i = 10;
		while (existsNaturalId(candidate + i)) {
			int digits = 2;
			i = CodecUtils.randomInt(digits);
		}
		return candidate + i;
	}

	@Override
	protected Set<String> getBuiltInRoles() {
		return Collections.singleton(UserRole.ROLE_BUILTIN_USER);
	}

}
