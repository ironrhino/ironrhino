package org.ironrhino.sample.api.controller;

import java.util.concurrent.ExecutorService;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.security.role.UserRole;
import org.ironrhino.core.util.AuthzUtils;
import org.ironrhino.core.util.BeanUtils;
import org.ironrhino.rest.Asserts;
import org.ironrhino.rest.RestStatus;
import org.ironrhino.security.model.User;
import org.ironrhino.security.service.UserManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

@RestController
@RequestMapping("/user")
@Authorize(ifAnyGranted = UserRole.ROLE_ADMINISTRATOR)
public class UserController {

	@Autowired
	private UserManager userManager;

	@Autowired
	private ExecutorService executorService;

	@RequestMapping(value = "/@self", method = RequestMethod.GET)
	@Authorize(ifAnyGranted = { UserRole.ROLE_BUILTIN_USER, UserRole.ROLE_BUILTIN_ANONYMOUS })
	public User self() {
		User user = AuthzUtils.getUserDetails(User.class);
		if (user == null)
			throw RestStatus.NOT_FOUND;
		return user;
	}

	@RequestMapping(value = "/@self", method = RequestMethod.PATCH)
	@Authorize(ifAnyGranted = UserRole.ROLE_BUILTIN_USER)
	public RestStatus patch(@RequestBody User user) {
		return patch(AuthzUtils.getUsername(), user);
	}

	@RequestMapping(value = "/@self/password", method = RequestMethod.PATCH)
	@Authorize(ifAnyGranted = UserRole.ROLE_BUILTIN_USER)
	public RestStatus validatePassword(@RequestBody User user) {
		boolean valid = AuthzUtils.isPasswordValid(user.getPassword());
		return valid ? RestStatus.OK : RestStatus.valueOf(RestStatus.CODE_FIELD_INVALID, "password invalid");
	}

	// @RequestMapping(value = "/{username}", method = RequestMethod.GET)
	// public User get(@PathVariable String username) {
	// User u = (User) userManager.loadUserByUsername(username);
	// if (u == null)
	// throw RestStatus.NOT_FOUND;
	// return u;
	// }

	@RequestMapping(value = "/{username}", method = RequestMethod.GET)
	public DeferredResult<User> get(final @PathVariable String username) {
		final DeferredResult<User> dr = new DeferredResult<>(5000L, RestStatus.REQUEST_TIMEOUT);
		executorService.submit(() -> {
			User u = (User) userManager.loadUserByUsername(username);
			if (u == null)
				dr.setErrorResult(RestStatus.NOT_FOUND);
			dr.setResult(u);
		});
		return dr;
	}

	@RequestMapping(method = RequestMethod.POST)
	public RestStatus post(@RequestBody User user) {
		Asserts.notBlank(user, "username", "password", "name");
		User u = (User) userManager.loadUserByUsername(user.getUsername());
		if (u != null)
			throw RestStatus.valueOf(RestStatus.CODE_ALREADY_EXISTS, "username already exists");
		if (StringUtils.isNotBlank(user.getEmail())) {
			u = (User) userManager.loadUserByUsername(user.getEmail());
			if (u != null)
				throw RestStatus.valueOf(RestStatus.CODE_ALREADY_EXISTS, "email already exists");
		}
		u = new User();
		u.setUsername(user.getUsername());
		u.setLegiblePassword(user.getPassword());
		BeanUtils.copyPropertiesIfNotNull(user, u, "name", "email", "phone");
		userManager.save(u);
		return RestStatus.OK;
	}

	@RequestMapping(value = "/{username}", method = RequestMethod.PATCH)
	public RestStatus patch(@PathVariable String username, @RequestBody User user) {
		User u = (User) userManager.loadUserByUsername(username);
		if (u == null) {
			// throw RestStatus.NOT_FOUND;
			u = new User();
			u.setUsername(username);
		}
		if (user.getPassword() != null)
			u.setLegiblePassword(user.getPassword());
		BeanUtils.copyPropertiesIfNotNull(user, u, "name", "email", "phone");
		userManager.save(u);
		return RestStatus.OK;
	}

	@RequestMapping(value = "/{username}", method = RequestMethod.DELETE)
	public RestStatus delete(@PathVariable String username) {
		User u = (User) userManager.loadUserByUsername(username);
		if (u == null)
			throw RestStatus.NOT_FOUND;
		if (u.isEnabled())
			throw RestStatus.FORBIDDEN;
		userManager.delete(u);
		return RestStatus.OK;
	}

	@RequestMapping(value = "/{username}/password", method = RequestMethod.PATCH)
	public RestStatus validatePassword(@PathVariable String username, @RequestBody User user) {
		User u = (User) userManager.loadUserByUsername(username);
		if (u == null)
			throw RestStatus.valueOf(RestStatus.CODE_FIELD_INVALID, "username invalid");
		boolean valid = AuthzUtils.isPasswordValid(u, user.getPassword());
		return valid ? RestStatus.OK : RestStatus.valueOf(RestStatus.CODE_FIELD_INVALID, "password invalid");
	}

}
