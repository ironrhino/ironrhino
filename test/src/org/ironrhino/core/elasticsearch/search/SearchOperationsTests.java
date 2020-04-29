package org.ironrhino.core.elasticsearch.search;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.ironrhino.core.elasticsearch.Article;
import org.ironrhino.core.elasticsearch.ArticleOperations;
import org.ironrhino.core.elasticsearch.index.Configuration;
import org.ironrhino.core.elasticsearch.index.Field;
import org.ironrhino.core.elasticsearch.index.Mappings;
import org.ironrhino.core.elasticsearch.search.SearchOperationsTests.Config;
import org.ironrhino.rest.client.RestApiRegistryPostProcessor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = Config.class)
public class SearchOperationsTests {

	@Autowired
	private ArticleOperations articleOperations;

	@Test
	public void test() {
		String index = "article";
		if (articleOperations.exists(index))
			articleOperations.delete(index);
		Configuration conf = new Configuration();
		Map<String, Field> properties = new HashMap<>();
		properties.put("id", new Field("keyword"));
		properties.put("title", new Field("keyword"));
		conf.setMappings(new Mappings(properties));
		articleOperations.create(index, conf);
		int size = 20;
		for (int i = 0; i < size; i++) {
			Article article = new Article(String.valueOf(i + 1), i % 2 == 0 ? "title" : "test", "content", i);
			articleOperations.index(index, article.getId(), article);
		}
		try {
			TimeUnit.MILLISECONDS.sleep(1000); // wait for index completion
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		List<SearchHits<Article>> list = articleOperations.search(index, "title:title");
		assertThat(list.size(), is(10));
		list = articleOperations.search(index, "title:test");
		assertThat(list.size(), is(10));
		list = articleOperations.search(index, "content");
		assertThat(list.size(), is(10)); // default size is 10

		list = articleOperations.search(index, "title:title", 0, 5);
		assertThat(list.size(), is(5));
		list = articleOperations.search(index, "title:test", 0, 5);
		assertThat(list.size(), is(5));
		list = articleOperations.search(index, "content", 0, 20);
		assertThat(list.size(), is(20));
		list = articleOperations.search(index, "content", 10, 20);
		assertThat(list.size(), is(10));

		List<AggregationBucket> buckets = articleOperations.aggregate(index, TermsAggregation.of("title"));
		assertThat(buckets.size(), is(2));
		assertThat(buckets.get(0).getKey(), is("test"));
		assertThat(buckets.get(0).getCount(), is(10));
		assertThat(buckets.get(1).getKey(), is("title"));
		assertThat(buckets.get(1).getCount(), is(10));

		buckets = articleOperations.aggregate(index, HistogramAggregation.of("views", 2));
		assertThat(buckets.size(), is(10));
		assertThat(buckets.get(0).getKey(), is("0.0"));
		assertThat(buckets.get(0).getCount(), is(2));
		assertThat(buckets.get(9).getKey(), is("18.0"));
		assertThat(buckets.get(9).getCount(), is(2));
		for (int i = 0; i < size; i++) {
			articleOperations.delete(index, String.valueOf(i + 1));
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
