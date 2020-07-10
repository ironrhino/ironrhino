package org.ironrhino.rest.client;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.ironrhino.core.model.ResultPage;
import org.ironrhino.rest.RestStatus;
import org.ironrhino.sample.api.model.Article;
import org.ironrhino.security.domain.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpClientErrorException;

import com.fasterxml.jackson.databind.JsonNode;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = RestClientConfiguration.class)
public class RestApiTests {

	@Autowired
	private UserClient userClient;

	@Autowired
	private UploadClient uploadClient;

	@Autowired
	private DownloadClient downloadClient;

	@Autowired
	private ArticleClient articleClient;

	@Test
	public void testGetAndPatch() {
		assertThat(userClient.self().getUsername(), is("admin"));
		assertThat(userClient.all().size() > 0, is(true));
		ResultPage<User> page = userClient.paged(1, 1);
		assertThat(page.getPageNo(), is(1));
		assertThat(page.getPageSize(), is(1));
		assertThat(page.getResult().size(), is(1));
		assertThat(userClient.get("admin").getUsername(), is("admin"));
		User u = new User();
		String newName = "admin" + new Random().nextInt(1000);
		u.setName(newName);
		userClient.patch(u);
		assertThat(userClient.self().getName(), is(newName));
	}

	@Test
	public void testPaged() {
		ResultPage<User> resultPage = userClient.paged(1, 1);
		assertThat(resultPage.getPageNo(), is(1));
		assertThat(resultPage.getPageSize(), is(1));
		assertThat(resultPage.getResult().size(), is(1));
		resultPage = userClient.pagedRestResult(1, 1);
		assertThat(resultPage.getPageNo(), is(1));
		assertThat(resultPage.getPageSize(), is(1));
		assertThat(resultPage.getResult().size(), is(1));
		resultPage = userClient.pagedRestResultWithResponseEntity(1, 1).getBody();
		assertThat(resultPage, notNullValue());
		assertThat(resultPage.getPageNo(), is(1));
		assertThat(resultPage.getPageSize(), is(1));
		assertThat(resultPage.getResult().size(), is(1));
	}

	@Test
	public void testPostForm() {
		Article article = new Article();
		article.setId(100);
		article.setAuthor("测试");
		article.setPublishDate(LocalDate.of(2000, 10, 12));
		assertThat(articleClient.postForm(article), is(article));
	}

	@Test(expected = RestStatus.class)
	public void testThrowRestStatus() {
		articleClient.view(100);
	}

	@Test
	public void testJsonPointer() {
		assertThat(userClient.pagedResult(1, 1).size(), is(1));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testJsonPointerWithValidator() {
		userClient.pagedResultWithValidator(1, 1);
	}

	@Test
	public void testValidatePassword() {
		User u = new User();
		u.setPassword("password");
		assertThat(userClient.validatePassword(u), is(RestStatus.OK));
		u.setPassword("password2");
		assertThat(userClient.validatePassword(u).getCode(), is(RestStatus.CODE_FIELD_INVALID));
	}

	@Test
	public void testThrows() {
		RestStatus rs = null;
		try {
			userClient.get("usernamenotexists");
		} catch (RestStatus e) {
			rs = e;
		}
		assertThat(rs, is(notNullValue()));
		assertThat(RestStatus.CODE_NOT_FOUND, is(rs.getCode()));
		assertThat(rs.getCause() instanceof HttpClientErrorException, is(true));
	}

	@Test
	public void testGetStream() throws IOException {
		InputStream is = userClient.getStream();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
			List<String> lines = br.lines().collect(Collectors.toList());
			assertThat(lines.isEmpty(), is(false));
		}
	}

	@Test
	public void testPostText() {
		assertThat(uploadClient.uploadText("test".getBytes()), is("text:test"));
	}

	@Test
	public void testPostStream() {
		assertThat(uploadClient.uploadStream(new ByteArrayInputStream("test".getBytes())), is("stream:test"));
	}

	@Test
	public void testPostByteArray() {
		assertThat(uploadClient.uploadBytes("test".getBytes()), is("stream:test"));
	}

	@Test
	public void testUpload() throws IOException {
		JsonNode jn = uploadClient.upload("build", new File("build.xml"));
		assertThat(jn, notNullValue());
		JsonNode name = jn.get("name");
		JsonNode filename = jn.get("filename");
		JsonNode originalFilename = jn.get("originalFilename");
		assertThat(name, notNullValue());
		assertThat(name.asText(), is("build"));
		assertThat(filename, notNullValue());
		assertThat(filename.asText(), is("file"));
		assertThat(originalFilename, notNullValue());
		assertThat(originalFilename.asText(), is("build.xml"));
	}

	@Test
	public void testDownload() throws IOException {
		String filename = "build.xml";

		ResponseEntity<Resource> response = downloadClient.download(filename);
		assertThat(response.getStatusCode(), is(HttpStatus.OK));
		assertThat(response.getHeaders().getContentDisposition().getFilename(), containsString(filename));
		Resource resource = response.getBody();
		assertThat(resource, notNullValue());
		assertThat(resource.isReadable(), is(true));
		assertStreamMatchesContent(resource.getInputStream(), filename);

		ResponseEntity<InputStream> response2 = downloadClient.downloadStream(filename);
		assertThat(response2.getStatusCode(), is(HttpStatus.OK));
		assertThat(response2.getHeaders().getContentDisposition().getFilename(), containsString(filename));
		assertStreamMatchesContent(response2.getBody(), filename);

		resource = downloadClient.downloadDirectResource(filename);
		assertThat(resource.isReadable(), is(true));
		assertStreamMatchesContent(resource.getInputStream(), filename);

		InputStream stream = downloadClient.downloadDirectStream(filename);
		assertThat(stream, notNullValue());
		assertStreamMatchesContent(stream, filename);
	}

	private void assertStreamMatchesContent(InputStream stream, String content) throws IOException {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
			assertThat(br.lines().collect(Collectors.joining("\n")), is(content));
		}
	}

}
