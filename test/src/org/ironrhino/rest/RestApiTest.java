package org.ironrhino.rest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.ironrhino.rest.MockMvcResultMatchers.jsonPoint;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.spy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Collection;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.ironrhino.core.spring.http.client.RestTemplate;
import org.ironrhino.rest.RestApiTest.RestApiConfiguration;
import org.ironrhino.rest.client.ArticleClient;
import org.ironrhino.rest.client.DownloadClient;
import org.ironrhino.rest.client.RestApiFactoryBean;
import org.ironrhino.rest.client.UploadClient;
import org.ironrhino.sample.api.controller.ArticleController;
import org.ironrhino.sample.api.controller.DownloadController;
import org.ironrhino.sample.api.controller.UploadController;
import org.ironrhino.sample.api.model.Article;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.client.MockMvcClientHttpRequestFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.fasterxml.jackson.databind.JsonNode;

@RunWith(SpringRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = RestApiConfiguration.class)
public class RestApiTest {

	@Autowired
	private ArticleController articleController;
	@Autowired
	private MockMvc mockMvc;

	private ArticleClient articleClient;
	private UploadClient uploadClient;
	private DownloadClient downloadClient;

	@PostConstruct
	public void afterPropertiesSet() throws Exception {
		RestTemplate restTemplate = new RestTemplate(new MockMvcClientHttpRequestFactory(mockMvc));
		articleClient = RestApiFactoryBean.create(ArticleClient.class, restTemplate);
		uploadClient = RestApiFactoryBean.create(UploadClient.class, restTemplate);
		downloadClient = RestApiFactoryBean.create(DownloadClient.class, restTemplate);
	}

	@Test
	public void testGet() {
		Article article = articleClient.view(1);
		assertThat(article.getId(), is(1));
		assertThat(article.getAuthor(), is("Author1"));
		assertThat(article.getPublishDate(), is(notNullValue()));
	}

	@Test
	public void testReturnCollection() {
		Collection<Article> articles = articleClient.list();
		assertThat(articles.size(), is(10));
		int i = 1;
		for (Article article : articles) {
			assertThat(article.getId(), is(i));
			assertThat(article.getTitle(), is("Title" + i));
			assertThat(article.getAuthor(), is("Author" + (i++)));
		}
	}

	@Test
	public void testPostForm() {
		assertThat(articleClient.postForm(null), is(new Article()));
		then(articleController).should().postForm(new Article());

		Article article = new Article();
		article.setId(1024);
		article.setTitle("RestApi");
		article.setPublishDate(LocalDate.of(2019, 2, 26));
		assertThat(articleClient.postForm(article), is(article));
	}

	@Test
	public void testThrowException() {
		Article article = new Article();
		article.setTitle("exception");
		given(articleController.postForm(article)).willThrow(new RuntimeException("exception"));
		RestStatus e = null;
		try {
			articleClient.postForm(article);
		} catch (RestStatus restStatus) {
			e = restStatus;
		}
		assertThat(e, is(notNullValue()));
		assertThat(e.getCode(), is(RestStatus.CODE_INTERNAL_SERVER_ERROR));
		assertThat(e.getCause() instanceof RuntimeException, is(true));
		then(articleController).should().postForm(article);
	}

	@Test
	public void testThrowRestStatus() {
		RestStatus e = null;
		try {
			articleClient.view(100);
		} catch (RestStatus restStatus) {
			e = restStatus;
		}
		assertThat(e, is(notNullValue()));
		assertThat(e.getCode(), is(RestStatus.CODE_NOT_FOUND));
		assertThat(e.getCause() instanceof HttpClientErrorException, is(true));
	}

	@Test
	public void testPostStream() {
		assertThat(uploadClient.upload(new ByteArrayInputStream("test".getBytes())), is("test"));
	}

	@Test
	public void testPostByteArray() {
		assertThat(uploadClient.upload("test".getBytes()), is("test"));
	}

	@Test
	@Ignore
	public void testUpload() {
		JsonNode result = uploadClient.upload("test", new File("build.xml"));
		assertThat(result.get("name").textValue(), is("build"));
		assertThat(result.get("filename").textValue(), is("file"));
		assertThat(result.get("originalFileName").textValue(), is("build.xml"));
	}

	@Test
	public void testMultipart() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "build.xml", MediaType.TEXT_PLAIN_VALUE,
				new FileInputStream("build.xml"));
		mockMvc.perform(multipart("/upload").file(file).param("name", "build")).andExpect(status().isOk())
				.andExpect(jsonPoint("/name").value("build")).andExpect(jsonPoint("/filename").value("file"))
				.andExpect(jsonPoint("/originalFilename").value("build.xml"));
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

	@EnableWebMvc
	static class RestApiConfiguration extends AbstractMockMvcConfigurer {

		@Bean
		public ArticleController articleController() {
			return spy(new ArticleController());
		}

		@Bean
		public UploadController uploadController() {
			return new UploadController();
		}

		@Bean
		public DownloadController downloadController() {
			return new DownloadController();
		}

	}

}
