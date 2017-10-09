package org.ironrhino.rest.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Random;

import org.apache.commons.io.IOUtils;
import org.ironrhino.rest.RestStatus;
import org.ironrhino.security.domain.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

@RunWith(SpringRunner.class)
@ContextConfiguration(locations = { "ctx.xml" })
public class RestApiTests {

	@Autowired
	private UserClient userClient;

	@Test
	public void testGetAndPatch() {
		assertEquals("admin", userClient.self().getUsername());
		assertTrue(userClient.all().size() > 0);
		assertEquals("admin", userClient.get("admin").getUsername());
		User u = new User();
		String newName = "admin" + new Random().nextInt(1000);
		u.setName(newName);
		userClient.patch(u);
		assertEquals(newName, userClient.self().getName());
	}

	@Test
	public void testValidatePassword() {
		User u = new User();
		u.setPassword("password");
		assertEquals(RestStatus.OK, userClient.validatePassword(u));
		u.setPassword("password2");
		assertEquals(RestStatus.CODE_FIELD_INVALID, userClient.validatePassword(u).getCode());
	}

	@Test(expected = HttpClientErrorException.class)
	public void testThrows() {
		userClient.get("usernamenotexists");
	}

	@Test(expected = HttpServerErrorException.class)
	public void testPostStream() {
		InputStream is = new ByteArrayInputStream("test".getBytes());
		userClient.postStream(is);
	}

	@Test
	public void testGetStream() throws IOException {
		InputStream is = userClient.getStream();
		List<String> lines = IOUtils.readLines(is);
		assertEquals(false, lines.isEmpty());
	}

}
