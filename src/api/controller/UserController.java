package api.controller;

import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.security.role.UserRole;
import org.ironrhino.core.util.AuthzUtils;
import org.ironrhino.security.model.User;
import org.ironrhino.security.service.UserManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import api.RestStatus;

@RestController
@RequestMapping("/user")
@Authorize(ifAnyGranted = UserRole.ROLE_ADMINISTRATOR)
public class UserController {

	@Autowired
	private UserManager userManager;

	@RequestMapping(value = "/@self", method = RequestMethod.GET)
	@Authorize(ifAnyGranted = UserRole.ROLE_BUILTIN_USER)
	public User self() {
		return AuthzUtils.getUserDetails();
	}

	@RequestMapping(value = "/{username}", method = RequestMethod.GET)
	public User get(@PathVariable String username) {
		User u = (User) userManager.loadUserByUsername(username);
		if (u == null)
			throw RestStatus.NOT_FOUND;
		return u;
	}

	@RequestMapping(method = RequestMethod.POST)
	public RestStatus post(@RequestBody User user) {
		User u = (User) userManager.loadUserByUsername(user.getUsername());
		if (u != null)
			throw RestStatus
					.valueOf(RestStatus.CODE_ALREADY_EXISTS, "username");
		u = new User();
		u.setUsername(user.getUsername());
		u.setLegiblePassword(user.getPassword());
		u.setName(user.getName());
		u.setEmail(user.getEmail());
		u.setPhone(user.getPhone());
		userManager.save(u);
		return RestStatus.OK;
	}

	@RequestMapping(value = "/{username}", method = RequestMethod.PUT)
	public RestStatus put(@PathVariable String username, @RequestBody User user) {
		User u = (User) userManager.loadUserByUsername(username);
		if (u == null)
			throw RestStatus.NOT_FOUND;
		u.setLegiblePassword(user.getPassword());
		u.setName(user.getName());
		u.setEmail(user.getEmail());
		u.setPhone(user.getPhone());
		userManager.save(u);
		return RestStatus.OK;
	}

	@RequestMapping(value = "/{username}", method = RequestMethod.DELETE)
	public RestStatus delete(@PathVariable String username) {
		User u = (User) userManager.loadUserByUsername(username);
		if (u == null)
			throw RestStatus.NOT_FOUND;
		if (!u.isEnabled()) {
			userManager.delete(u);
			return RestStatus.OK;
		} else {
			return RestStatus.ACCESS_UNAUTHORIZED;
		}
	}

}
