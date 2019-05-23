package org.ironrhino.rest.client;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;

import org.ironrhino.core.util.JsonUtils;
import org.ironrhino.security.domain.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = RestClientConfiguration.class)
public class RestClientTests {

	@Autowired
	private RestClient restClient;

	@Test
	public void testFetchAccessToken() {
		String accessToken = restClient.fetchAccessToken();
		assertThat(accessToken, is(notNullValue()));
		String accessToken2 = restClient.fetchAccessToken();
		assertThat(accessToken2, is(notNullValue()));
		assertThat(accessToken2, is(accessToken));
	}

	@Test
	public void testRestTemplate() {
		RestTemplate rt = restClient.getRestTemplate();
		User u = rt.getForObject("/user/@self", User.class);
		assertThat(u, is(notNullValue()));
		assertThat(u.getUsername(), is(notNullValue()));
		u = rt.getForObject("/user/admin", User.class);
		assertThat(u, is(notNullValue()));
		assertThat(u.getUsername(), is("admin"));
	}

	@Test
	public void testUploadFile() throws IOException {
		RestTemplate rt = restClient.getRestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
		params.add("name", "build");
		params.add("file", new FileSystemResource("build.xml"));
		HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(params, headers);
		String response = rt.postForEntity("/upload", request, String.class).getBody();
		JsonNode jn = JsonUtils.fromJson(response, JsonNode.class);
		assertThat(jn.get("name").asText(), is("build"));
		assertThat(jn.get("filename").asText(), is("file"));
		assertThat(jn.get("originalFilename").asText(), is("build.xml"));
	}

}
