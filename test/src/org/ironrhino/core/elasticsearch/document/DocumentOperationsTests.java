package org.ironrhino.core.elasticsearch.document;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.ironrhino.core.elasticsearch.document.DocumentOperationsTests.Config;
import org.ironrhino.rest.client.RestApiRegistryPostProcessor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = Config.class)
public class DocumentOperationsTests {

	@Autowired
	private ArticleOperations articleOperations;

	@Test
	public void test() {
		String index = "article";
		Article article = new Article("id", "title", "content");
		articleOperations.index(index, article.getId(), article);
		assertThat(articleOperations.get(index, article.getId()), is(article));
		article.setContent("content2");
		articleOperations.update(index, article.getId(), article);
		assertThat(articleOperations.get(index, article.getId()), is(article));
		articleOperations.delete(index, article.getId());
		assertThat(articleOperations.get(index, article.getId()), is(nullValue()));

	}

	static class Config {
		@Bean
		public static RestApiRegistryPostProcessor restApiRegistryPostProcessor() {
			RestApiRegistryPostProcessor obj = new RestApiRegistryPostProcessor();
			obj.setAnnotatedClasses(new Class[] { ArticleOperations.class });
			return obj;
		}
	}

}
