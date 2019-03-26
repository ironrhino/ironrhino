package org.ironrhino.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.ironrhino.core.spring.http.client.RestTemplate;
import org.ironrhino.core.util.JsonUtils;
import org.ironrhino.rest.client.ArticleClient;
import org.ironrhino.rest.client.RestApiFactoryBean;
import org.ironrhino.rest.client.UploadClient;
import org.ironrhino.sample.api.controller.ArticleController;
import org.ironrhino.sample.api.model.Article;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.client.MockMvcClientHttpRequestFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.JsonNode;

@RunWith(SpringRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = RestApiConfiguration.class)
public class RestApiTest {

	private static final Article EMPTY_ARTICLE = new Article();

	@Autowired
	private WebApplicationContext wac;
	@Autowired
	private ArticleController articleController;

	private MockMvc mockMvc;
	private ArticleClient articleClient;
	private UploadClient uploadClient;

	@PostConstruct
	public void afterPropertiesSet() throws Exception {
		mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
		RestTemplate restTemplate = new RestTemplate(new MockMvcClientHttpRequestFactory(mockMvc));
		articleClient = RestApiFactoryBean.create(ArticleClient.class, restTemplate);
		uploadClient = RestApiFactoryBean.create(UploadClient.class, restTemplate);
	}

	@Before
	public void reset() {
		Mockito.reset(articleController);
	}

	@Test
	public void testGet() {
		Article article = articleClient.view(1);
		assertSame(1, article.getId());
		assertEquals("Author1", article.getAuthor());
		assertNotNull(article.getPublishDate());
	}

	@Test
	public void testReturnCollection() {
		Collection<Article> articles = articleClient.list();
		assertEquals(10, articles.size());
		int i = 1;
		for (Article article : articles) {
			assertSame(i, article.getId());
			assertEquals("Title" + i, article.getTitle());
			assertEquals("Author" + (i++), article.getAuthor());
		}
	}

	@Test
	public void testPostForm() {
		assertEquals(EMPTY_ARTICLE, articleClient.postForm(null));
		verify(articleController).postForm(argThat(p -> EMPTY_ARTICLE.equals(p)));

		Article article = new Article();
		article.setId(1024);
		article.setTitle("RestApi");
		article.setPublishDate(LocalDate.of(2019, 2, 26));
		assertEquals(article, articleClient.postForm(article));
	}

	@Test
	public void testThrowException() {
		doThrow(new RuntimeException("test")).when(articleController).postForm(argThat(p -> EMPTY_ARTICLE.equals(p)));
		RestStatus e = null;
		try {
			articleClient.postForm(new Article());
		} catch (RestStatus restStatus) {
			e = restStatus;
		}
		assertNotNull(e);
		assertEquals(RestStatus.CODE_INTERNAL_SERVER_ERROR, e.getCode());
		assertTrue(e.getCause() instanceof RuntimeException);
		verify(articleController).postForm(argThat(p -> EMPTY_ARTICLE.equals(p)));
	}

	@Test
	public void testThrowRestStatus() {
		RestStatus e = null;
		try {
			articleClient.view(100);
		} catch (RestStatus restStatus) {
			e = restStatus;
		}
		assertNotNull(e);
		assertEquals(RestStatus.CODE_NOT_FOUND, e.getCode());
		assertTrue(e.getCause() instanceof HttpClientErrorException);
	}

	@Test
	public void testPostStream() {
		assertEquals("test", uploadClient.upload(new ByteArrayInputStream("test".getBytes())));
	}

	@Test
	public void testPostByteArray() {
		assertEquals("test", uploadClient.upload("test".getBytes()));
	}

	@Test
	@Ignore
	public void testUpload() {
		Map<String, String> result = uploadClient.upload("test", new File("build.xml"));
		assertEquals("build", result.get("name"));
		assertEquals("file", result.get("filename"));
		assertEquals("build.xml", result.get("originalFileName"));
	}

	@Test
	public void testMultipart() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "build.xml", MediaType.TEXT_PLAIN_VALUE,
				new FileInputStream("build.xml"));
		MvcResult mvcResult = mockMvc.perform(multipart("/upload").file(file).param("name", "build"))
				.andExpect(status().isOk()).andReturn();
		JsonNode jn = JsonUtils.fromJson(mvcResult.getResponse().getContentAsString(), JsonNode.class);
		assertEquals("build", jn.get("name").asText());
		assertEquals("file", jn.get("filename").asText());
		assertEquals("build.xml", jn.get("originalFilename").asText());
	}
}
