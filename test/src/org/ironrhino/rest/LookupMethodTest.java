package org.ironrhino.rest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.spy;

import javax.annotation.PostConstruct;

import org.ironrhino.core.spring.http.client.RestTemplate;
import org.ironrhino.rest.LookupMethodTest.RestApiConfiguration;
import org.ironrhino.rest.client.RestApi;
import org.ironrhino.rest.client.RestApiFactoryBean;
import org.ironrhino.sample.api.controller.ArticleController;
import org.ironrhino.sample.api.model.Article;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.client.MockMvcClientHttpRequestFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@RunWith(SpringRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = RestApiConfiguration.class)
public class LookupMethodTest {

	@Autowired
	private ArticleController articleController;
	@Autowired
	private MockMvc mockMvc;

	private ArticleClient articleClient;

	@PostConstruct
	public void afterPropertiesSet() throws Exception {
		RestTemplate restTemplate = new RestTemplate(new MockMvcClientHttpRequestFactory(mockMvc));
		articleClient = RestApiFactoryBean.create(ArticleClient.class, restTemplate);
	}

	@Test
	public void testGet() {
		Article article = articleClient.view(1);
		assertThat(article.getId(), is(1));
		assertThat(article.getAuthor(), is("Author1"));
		assertThat(article.getPublishDate(), is(notNullValue()));
		then(articleController).should().view(1);
	}

	@EnableWebMvc
	static class RestApiConfiguration extends AbstractMockMvcConfigurer {

		@Bean
		public ArticleController articleController() {
			return spy(new ArticleController());
		}

	}

	@RestApi
	interface ArticleClient {

		@Lookup
		org.springframework.web.client.RestTemplate restTemplate();

		default Article view(Integer id) {
			return restTemplate().getForEntity("/article/{id}", Article.class, id).getBody();
		}

	}

}
