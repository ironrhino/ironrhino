package org.ironrhino.api.controller;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.api.Asserts;
import org.ironrhino.api.RestStatus;
import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.security.role.UserRole;
import org.ironrhino.core.util.AuthzUtils;
import org.ironrhino.core.util.BeanUtils;
import org.ironrhino.security.model.User;
import org.ironrhino.security.oauth.server.component.OAuthHandler;
import org.ironrhino.security.oauth.server.model.Authorization;
import org.ironrhino.security.oauth.server.model.Client;
import org.ironrhino.security.oauth.server.service.OAuthManager;
import org.ironrhino.security.service.UserManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@Authorize(ifAnyGranted = UserRole.ROLE_ADMINISTRATOR)
public class UserController {

	@Autowired
	private UserManager userManager;

	@Autowired
	private OAuthManager oauthManager;

	@RequestMapping(value = "/@self", method = RequestMethod.GET)
	@Authorize(ifAnyGranted = UserRole.ROLE_BUILTIN_USER)
	public User self() {
		return AuthzUtils.getUserDetails();
	}

	@RequestMapping(value = "/@self", method = RequestMethod.PUT)
	@Authorize(ifAnyGranted = UserRole.ROLE_BUILTIN_USER)
	public RestStatus put(@RequestBody User user) {
		return put(AuthzUtils.getUsername(), user);
	}

	@RequestMapping(value = "/@self/password", method = RequestMethod.PATCH)
	@Authorize(ifAnyGranted = UserRole.ROLE_BUILTIN_USER)
	public RestStatus validatePassword(@RequestBody User user) {
		boolean valid = AuthzUtils.isPasswordValid(user.getPassword());
		return valid ? RestStatus.OK : RestStatus.valueOf(
				RestStatus.CODE_FIELD_INVALID, "password invalid");
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
		Asserts.notBlank(user, "username", "password", "name");
		User u = (User) userManager.loadUserByUsername(user.getUsername());
		if (u != null)
			throw RestStatus.valueOf(RestStatus.CODE_ALREADY_EXISTS,
					"username already exists");
		if (StringUtils.isNotBlank(user.getEmail())) {
			u = (User) userManager.loadUserByUsername(user.getEmail());
			if (u != null)
				throw RestStatus.valueOf(RestStatus.CODE_ALREADY_EXISTS,
						"email already exists");
		}
		u = new User();
		u.setUsername(user.getUsername());
		u.setLegiblePassword(user.getPassword());
		BeanUtils.copyPropertiesIfNotNull(user, u, "name", "email", "phone");
		userManager.save(u);
		return RestStatus.OK;
	}

	@RequestMapping(value = "/{username}", method = RequestMethod.PUT)
	public RestStatus put(@PathVariable String username, @RequestBody User user) {
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
	public RestStatus validatePassword(@PathVariable String username,
			@RequestBody User user) {
		User u = (User) userManager.loadUserByUsername(username);
		if (u == null)
			throw RestStatus.valueOf(RestStatus.CODE_FIELD_INVALID,
					"username invalid");
		boolean valid = AuthzUtils.isPasswordValid(u, user.getPassword());
		return valid ? RestStatus.OK : RestStatus.valueOf(
				RestStatus.CODE_FIELD_INVALID, "password invalid");
	}

	@RequestMapping(value = "/authorization", method = RequestMethod.POST)
	public Map<String, Object> authorization(HttpServletRequest request,
			@RequestBody User user) {
		Client client = (Client) request
				.getAttribute(OAuthHandler.REQUEST_ATTRIBUTE_KEY_OAUTH_CLIENT);
		if (client == null)
			throw RestStatus.FORBIDDEN;
		Asserts.notBlank(user, "username", "password");
		User u = (User) userManager.loadUserByUsername(user.getUsername());
		if (u == null)
			throw RestStatus.valueOf(RestStatus.CODE_FIELD_INVALID,
					"username invalid");
		if (!AuthzUtils.isPasswordValid(u, user.getPassword()))
			throw RestStatus.valueOf(RestStatus.CODE_FIELD_INVALID,
					"password invalid");
		Map<String, Object> map = new HashMap<String, Object>();
		Authorization authorization = oauthManager.grant(client, u);
		map.put("expires_in", authorization.getExpiresIn());
		map.put("access_token", authorization.getAccessToken());
		map.put("refresh_token", authorization.getRefreshToken());
		return map;
	}

}
