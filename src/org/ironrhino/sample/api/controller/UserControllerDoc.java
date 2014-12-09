package org.ironrhino.sample.api.controller;

import org.ironrhino.core.util.JsonUtils;
import org.ironrhino.rest.RestStatus;
import org.ironrhino.rest.doc.annotation.Api;
import org.ironrhino.rest.doc.annotation.ApiModule;
import org.ironrhino.rest.doc.annotation.Field;
import org.ironrhino.rest.doc.annotation.Fields;
import org.ironrhino.security.model.User;
import org.springframework.web.context.request.async.DeferredResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@ApiModule(value = "用户API", description = "跟用户打交道的api")
public class UserControllerDoc extends UserController {

	@Override
	@Api("获取当前用户信息")
	@Fields({ @Field(name = "username", required = true),
			@Field(name = "email", label = "电子邮箱") })
	public User self() {
		User u = new User();
		u.setUsername("test");
		return u;
	}

	@Override
	@Api(value="修改当前用户信息",description = "<strong>只传入需要修改的字段</strong>")
	public RestStatus put(@Fields({
			@Field(name = "password", description = "传入明文密码"),
			@Field(name = "email", label = "电子邮箱") }) User user) {
		return RestStatus.OK;
	}

	@Override
	@Api("校验当前用户密码")
	public RestStatus validatePassword(
			@Fields(value = { @Field(name = "password", required = true, description = "传入明文密码") }, sampleMethodName = "createUserForPassword") User user) {
		return RestStatus.OK;
	}

	public JsonNode createUserForPassword() {
		ObjectNode jsonNode = JsonUtils.createNewObjectMapper()
				.createObjectNode();
		jsonNode.put("password", "iampassword");
		return jsonNode;
	}

	@Override
	@Api("根据用户名获取用户")
	public DeferredResult<User> get(
			@Field(label = "用户名", description = "不能为空") String username) {
		DeferredResult<User> result = new DeferredResult<User>();
		User u = new User();
		u.setUsername("test");
		result.setResult(u);
		return result;
	}

	@Override
	public RestStatus post(User user) {
		return RestStatus.OK;
	}

	@Override
	public RestStatus put(String username, User user) {
		return RestStatus.OK;
	}

	@Override
	public RestStatus delete(String username) {
		return RestStatus.OK;
	}

	@Override
	public RestStatus validatePassword(String username, User user) {
		return RestStatus.OK;
	}

}
