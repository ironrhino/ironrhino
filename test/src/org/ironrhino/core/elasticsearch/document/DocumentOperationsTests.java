package org.ironrhino.core.elasticsearch.document;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Date;

import org.ironrhino.core.elasticsearch.Article;
import org.ironrhino.core.elasticsearch.ArticleOperations;
import org.ironrhino.core.elasticsearch.document.DocumentOperationsTests.Config;
import org.ironrhino.rest.client.RestApiRegistryPostProcessor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpClientErrorException;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = Config.class)
public class DocumentOperationsTests {

	@Autowired
	private ArticleOperations articleOperations;

	@Test
	public void test() {
		String index = "article";
		Article article = new Article("id", "title", "content", 0, new Date());
		articleOperations.index(index, article.getId(), article);
		assertThat(articleOperations.get(index, article.getId()), is(article));
		article.setContent("content2");
		articleOperations.update(index, article.getId(), article);
		assertThat(articleOperations.get(index, article.getId()), is(article));
		articleOperations.delete(index, article.getId());
		assertThat(articleOperations.get(index, article.getId()), is(nullValue()));
		articleOperations.delete(index);
	}

	@Test(expected = HttpClientErrorException.Conflict.class)
	public void testPutIfAbsent() {
		String index = "article";
		Article article = new Article("id", "title", "content", 0, new Date());
		articleOperations.index(index, article.getId(), article);
		assertThat(articleOperations.get(index, article.getId()), is(article));
		article.setContent("content2");
		try {
			articleOperations.putIfAbsent(index, article.getId(), article);
		} finally {
			articleOperations.delete(index);
		}
	}

	@Test
	public void testCAS() {
		String index = "article";
		Article article = new Article("id", "title", "content", 0, new Date());
		articleOperations.index(index, article.getId(), article);
		Detail<Article> detail = articleOperations.detail(index, article.getId());
		assertThat(detail.getIndex(), is(index));
		assertThat(detail.getId(), is(article.getId()));
		assertThat(detail.getVersion(), is(1));
		assertThat(detail.getDocument(), is(article));
		article.setContent("content2");
		articleOperations.update(index, article.getId(), article, detail.getSeqNo(), detail.getPrimaryTerm());
		detail = articleOperations.detail(index, article.getId());
		assertThat(detail.getVersion(), is(2));
		assertThat(detail.getDocument(), is(article));
		articleOperations.delete(index);
	}

	@Test(expected = HttpClientErrorException.Conflict.class)
	public void testCASWithConflict() {
		String index = "article";
		Article article = new Article("id", "title", "content", 0, new Date());
		articleOperations.index(index, article.getId(), article);
		Detail<Article> detail = articleOperations.detail(index, article.getId());
		assertThat(detail.getIndex(), is(index));
		assertThat(detail.getId(), is(article.getId()));
		assertThat(detail.getVersion(), is(1));
		assertThat(detail.getDocument(), is(article));
		article.setContent("content2");
		try {
			articleOperations.update(index, article.getId(), article, 100, 100);
		} finally {
			articleOperations.delete(index);
		}
	}

	static class Config {
		@Bean
		public static RestApiRegistryPostProcessor restApiRegistryPostProcessor() {
			RestApiRegistryPostProcessor obj = new RestApiRegistryPostProcessor();
			obj.setPackagesToScan(new String[] { ArticleOperations.class.getPackage().getName() });
			return obj;
		}
	}

}
