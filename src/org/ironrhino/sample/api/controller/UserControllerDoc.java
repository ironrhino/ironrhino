package org.ironrhino.sample.api.controller;

import org.ironrhino.core.util.CodecUtils;
import org.ironrhino.rest.RestStatus;
import org.ironrhino.rest.doc.annotation.Api;
import org.ironrhino.rest.doc.annotation.ApiModule;
import org.ironrhino.rest.doc.annotation.Field;
import org.ironrhino.rest.doc.annotation.Fields;
import org.ironrhino.security.model.User;
import org.springframework.core.annotation.Order;
import org.springframework.web.context.request.async.DeferredResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@ApiModule(value = "用户API")
@Order(1)
public class UserControllerDoc extends UserController {

	@Override
	@Api("获取当前用户信息")
	public User self() {
		return createUserForGet();
	}

	@Override
	@Api(value = "修改当前用户信息", description = "只传入需要修改的字段")
	public RestStatus patch(@Fields(value = { @Field(name = "password", required = false, description = "传入明文密码"),
			@Field(name = "name", required = false), @Field(name = "email", required = false),
			@Field(name = "phone", required = false) }, sampleMethodName = "createUserForPut") User user) {
		return RestStatus.OK;
	}

	@Override
	@Api("校验当前用户密码")
	public RestStatus validatePassword(@Fields(value = {
			@Field(name = "password", description = "传入明文密码") }, sampleMethodName = "createUserForPassword") User user) {
		return RestStatus.OK;
	}

	protected JsonNode createUserForPassword() {
		ObjectNode jsonNode = new ObjectMapper().createObjectNode();
		jsonNode.put("password", "iampassword");
		return jsonNode;
	}

	@Override
	@Api("获取用户")
	public DeferredResult<User> get(String username) {
		DeferredResult<User> result = new DeferredResult<>();
		result.setResult(createUserForGet());
		return result;
	}

	@Override
	@Api("创建用户")
	public RestStatus post(@Fields(value = { @Field(name = "username", description = "字母和数字组合"),
			@Field(name = "password", description = "传入明文密码"), @Field(name = "name"),
			@Field(name = "email", required = false),
			@Field(name = "phone", required = false) }, sampleMethodName = "createUserForPost") User user) {
		return RestStatus.OK;
	}

	@Override
	@Api(value = "修改用户", description = "只传入需要修改的字段")
	public RestStatus patch(@Field(description = "字母和数字组合") String username,
			@Fields(value = { @Field(name = "password", required = false, description = "传入明文密码"),
					@Field(name = "name", required = false), @Field(name = "email", required = false),
					@Field(name = "phone", required = false) }, sampleMethodName = "createUserForPut") User user) {
		return RestStatus.OK;
	}

	@Override
	@Api(value = "删除用户", description = "只能删除已经禁用的用户")
	public RestStatus delete(String username) {
		return RestStatus.OK;
	}

	@Override
	@Api("校验用户密码")
	public RestStatus validatePassword(String username, @Fields(value = {
			@Field(name = "password", description = "传入明文密码") }, sampleMethodName = "createUserForPassword") User user) {
		return RestStatus.OK;
	}

	protected User createUserForGet() {
		User u = new User();
		u.setId(CodecUtils.nextId());
		u.setUsername("test");
		u.setName("测试");
		u.setEmail("test@test.com");
		u.setPhone("13111111111");
		return u;
	}

	protected ObjectNode createUserForPost() {
		ObjectNode jsonNode = new ObjectMapper().createObjectNode();
		jsonNode.put("username", "test123");
		jsonNode.put("password", "newpassword");
		jsonNode.put("name", "测试");
		jsonNode.put("email", "test@test.com");
		jsonNode.put("phone", "13111111111");
		return jsonNode;
	}

	protected User createUserForPut() {
		User u = new User();
		u.setPassword("newpassword");
		u.setName("测试");
		u.setEmail("test@test.com");
		u.setPhone("13111111111");
		return u;
	}

}
