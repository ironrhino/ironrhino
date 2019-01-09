package org.ironrhino.rest.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.ironrhino.core.model.ResultPage;
import org.ironrhino.rest.RestStatus;
import org.ironrhino.sample.api.model.Article;
import org.ironrhino.security.domain.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = RestClientConfiguration.class)
public class RestApiTests {

	@Autowired
	private UserClient userClient;

	@Autowired
	private UploadClient uploadClient;

	@Autowired
	private ArticleClient articleClient;

	@Test
	public void testGetAndPatch() {
		assertEquals("admin", userClient.self().getUsername());
		assertTrue(userClient.all().size() > 0);
		ResultPage<User> page = userClient.paged(1, 1);
		assertEquals(1, page.getPageNo());
		assertEquals(1, page.getPageSize());
		assertEquals(1, page.getResult().size());
		assertEquals("admin", userClient.get("admin").getUsername());
		User u = new User();
		String newName = "admin" + new Random().nextInt(1000);
		u.setName(newName);
		userClient.patch(u);
		assertEquals(newName, userClient.self().getName());
	}

	@Test
	public void testPostForm() {
		Article article = new Article();
		article.setId(100);
		article.setAuthor("测试");
		article.setPublishDate(LocalDate.of(2000, 10, 12));
		assertEquals(article, articleClient.postForm(article));
	}

	@Test(expected = RestStatus.class)
	public void testThrowRestStatus() {
		articleClient.view(100);
	}

	@Test
	public void testJsonPointer() {
		assertEquals(1, userClient.pagedResult(1, 1).size());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testJsonPointerWithValidator() {
		userClient.pagedResultWithValidator(1, 1);
	}

	@Test
	public void testValidatePassword() {
		User u = new User();
		u.setPassword("password");
		assertEquals(RestStatus.OK, userClient.validatePassword(u));
		u.setPassword("password2");
		assertEquals(RestStatus.CODE_FIELD_INVALID, userClient.validatePassword(u).getCode());
	}

	@Test
	public void testThrows() {
		RestStatus rs = null;
		try {
			userClient.get("usernamenotexists");
		} catch (RestStatus e) {
			rs = e;
		}
		assertEquals(rs.getCode(), RestStatus.CODE_NOT_FOUND);
		assertTrue(rs.getCause() instanceof HttpClientErrorException);
	}

	@Test
	public void testPostStream() {
		RestStatus rs = null;
		try {
			InputStream is = new ByteArrayInputStream("test".getBytes());
			userClient.postStream(is);
		} catch (RestStatus e) {
			rs = e;
		}
		assertEquals(rs.getCode(), RestStatus.CODE_INTERNAL_SERVER_ERROR);
		assertTrue(rs.getCause() instanceof HttpServerErrorException);
	}

	@Test
	public void testGetStream() throws IOException {
		InputStream is = userClient.getStream();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
			List<String> lines = br.lines().collect(Collectors.toList());
			assertEquals(false, lines.isEmpty());
		}
	}

	@Test
	public void testUpload() throws IOException {
		Map<String, String> result = uploadClient.upload("test", new File("build.xml"));
		assertEquals("test", result.get("name"));
		assertEquals("build.xml", result.get("originalFilename"));
	}

}
