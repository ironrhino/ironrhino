package org.ironrhino.security.component;

import org.ironrhino.core.metadata.Setup;
import org.ironrhino.core.metadata.SetupParameter;
import org.ironrhino.core.security.role.UserRole;
import org.ironrhino.core.util.CodecUtils;
import org.ironrhino.security.model.User;
import org.ironrhino.security.service.UserManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

@Component
public class AdminUserSetup {

	@Autowired
	private UserManager userManager;

	@Setup
	public User setup(
			@SetupParameter(defaultValue = "admin", displayOrder = Ordered.HIGHEST_PRECEDENCE, label = "admin.username") String username,
			@SetupParameter(defaultValue = "password", displayOrder = Ordered.HIGHEST_PRECEDENCE + 1, label = "admin.password") String password)
			throws Exception {
		if (userManager.countAll() > 0)
			return null;
		User admin = new User();
		admin.setUsername(username);
		admin.setPassword(CodecUtils.digest(password));
		admin.setEnabled(true);
		admin.getRoles().add(UserRole.ROLE_ADMINISTRATOR);
		userManager.save(admin);
		return admin;
	}

}
