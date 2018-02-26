package org.ironrhino.sample.api.controller;

import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.Length;
import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.model.ResultPage;
import org.ironrhino.core.security.role.UserRole;
import org.ironrhino.core.util.AuthzUtils;
import org.ironrhino.core.util.BeanUtils;
import org.ironrhino.rest.Asserts;
import org.ironrhino.rest.RestStatus;
import org.ironrhino.rest.doc.annotation.Api;
import org.ironrhino.rest.doc.annotation.Status;
import org.ironrhino.security.LoggedInUser;
import org.ironrhino.security.model.User;
import org.ironrhino.security.service.UserManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/user")
@Authorize(ifAnyGranted = UserRole.ROLE_ADMINISTRATOR)
@Validated
public class UserController {

	@Autowired
	private UserManager userManager;

	@Autowired
	private ExecutorService executorService;

	@Order(0)
	@Api("获取当前用户信息")
	@GetMapping(value = "/@self")
	@Authorize(ifAnyGranted = { UserRole.ROLE_BUILTIN_USER, UserRole.ROLE_BUILTIN_ANONYMOUS })
	public User self(@LoggedInUser User loggedInUser) {
		if (loggedInUser == null)
			throw RestStatus.NOT_FOUND;
		return loggedInUser;
	}

	@Order(1)
	@Api("获取所有用户信息")
	@RequestMapping(value = "/@all", method = RequestMethod.GET)
	public List<User> all() {
		return userManager.findAll();
	}

	@Order(2)
	@Api("分页获取所有用户信息")
	@RequestMapping(value = "/@paged", method = RequestMethod.GET)
	public ResultPage<User> paged(@RequestParam(defaultValue = "1") Integer pageNo,
			@RequestParam(defaultValue = "10") Integer pageSize) {
		ResultPage<User> rp = new ResultPage<>();
		if (pageNo != null && pageNo > 0)
			rp.setPageNo(pageNo);
		if (pageSize != null && pageSize > 0)
			rp.setPageSize(pageSize);
		return userManager.findByResultPage(rp);
	}

	@RequestMapping(value = "/@self", method = RequestMethod.PATCH)
	@Authorize(ifAnyGranted = UserRole.ROLE_BUILTIN_USER)
	public RestStatus patch(@LoggedInUser User loggedInUser, @Valid @RequestBody User user) {
		return patch(loggedInUser.getUsername(), user);
	}

	@RequestMapping(value = "/@self/password", method = RequestMethod.PATCH)
	@Authorize(ifAnyGranted = UserRole.ROLE_BUILTIN_USER)
	public RestStatus validatePassword(@RequestBody User user) {
		boolean valid = AuthzUtils.isPasswordValid(user.getPassword());
		return valid ? RestStatus.OK : RestStatus.valueOf(RestStatus.CODE_FIELD_INVALID, "password invalid");
	}

	@Order(3)
	@Api(value = "获取用户", statuses = { @Status(code = 404, description = "用户名不存在") })
	@RequestMapping(value = "/{username}", method = RequestMethod.GET)
	public DeferredResult<User> get(final @Length(min = 3, max = 20) @PathVariable String username) {
		final DeferredResult<User> dr = new DeferredResult<>(5000L, RestStatus.REQUEST_TIMEOUT);
		executorService.execute(() -> {
			User u = (User) userManager.loadUserByUsername(username);
			if (u == null)
				dr.setErrorResult(RestStatus.NOT_FOUND);
			dr.setResult(u);
		});
		return dr;
	}

	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public RestStatus post(@Valid @RequestBody User user) {
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
	public RestStatus patch(@Length(min = 3, max = 20) @PathVariable String username, @RequestBody User user) {
		User u = (User) userManager.loadUserByUsername(username);
		if (u == null) {
			throw RestStatus.NOT_FOUND;
		}
		if (user.getPassword() != null)
			u.setLegiblePassword(user.getPassword());
		BeanUtils.copyPropertiesIfNotNull(user, u, "name", "email", "phone");
		userManager.save(u);
		return RestStatus.OK;
	}

	@Order(6)
	@Api(value = "删除用户", description = "只能删除已经禁用的用户")
	@RequestMapping(value = "/{username}", method = RequestMethod.DELETE)
	public RestStatus delete(@Length(min = 3, max = 20) @PathVariable String username) {
		User u = (User) userManager.loadUserByUsername(username);
		if (u == null)
			throw RestStatus.NOT_FOUND;
		if (u.isEnabled())
			throw RestStatus.FORBIDDEN;
		userManager.delete(u);
		return RestStatus.OK;
	}

	@RequestMapping(value = "/{username}/password", method = RequestMethod.PATCH)
	public RestStatus validatePassword(@Length(min = 3, max = 20) @PathVariable String username,
			@RequestBody User user) {
		User u = (User) userManager.loadUserByUsername(username);
		if (u == null)
			throw RestStatus.valueOf(RestStatus.CODE_FIELD_INVALID, "username invalid");
		boolean valid = AuthzUtils.isPasswordValid(u, user.getPassword());
		return valid ? RestStatus.OK : RestStatus.valueOf(RestStatus.CODE_FIELD_INVALID, "password invalid");
	}

	@Api("flux示例")
	@GetMapping(value = "/flux")
	public Flux<User> flux() {
		return Flux.just(AuthzUtils.getUserDetails());
	}

	@Api("mono示例")
	@GetMapping(value = "/mono")
	public Mono<User> mono() {
		return Mono.just(AuthzUtils.getUserDetails());
	}

}
