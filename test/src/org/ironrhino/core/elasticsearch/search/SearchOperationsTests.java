package org.ironrhino.core.elasticsearch.search;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.ironrhino.core.elasticsearch.Article;
import org.ironrhino.core.elasticsearch.ArticleOperations;
import org.ironrhino.core.elasticsearch.Constants;
import org.ironrhino.core.elasticsearch.search.SearchOperationsTests.Config;
import org.ironrhino.core.util.DateUtils;
import org.ironrhino.core.util.NumberUtils;
import org.ironrhino.rest.client.RestApiRegistryPostProcessor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = Config.class)
@TestPropertySource(properties = Constants.KEY_ELASTICSEARCH_URL + "=http://localhost:9200")
public class SearchOperationsTests {

	@Autowired
	private ArticleOperations articleOperations;

	@Test
	public void test() {
		String index = "article";
		if (articleOperations.exists(index))
			articleOperations.delete(index);
		articleOperations.create(index);
		int size = 20;
		for (int i = 0; i < size; i++) {
			Article article = new Article(String.valueOf(i + 1), i % 2 == 0 ? "title" : "test", "content", i,
					DateUtils.parse("2020-05-" + NumberUtils.format(i + 1, 2)));
			articleOperations.index(index, article.getId(), article);
		}
		try {
			TimeUnit.MILLISECONDS.sleep(1000); // wait for index completion
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertThat(articleOperations.count(index, "title:title"), is(10L));
		List<SearchHit<Article>> list = articleOperations.search(index, "title:title").getHits().getHits();
		assertThat(list.size(), is(10));
		list = articleOperations.search(index, "title:test").getHits().getHits();
		assertThat(list.size(), is(10));
		list = articleOperations.search(index, "content").getHits().getHits();
		assertThat(list.size(), is(10)); // default size is 10

		list = articleOperations.search(index, "title:title", 0, 5).getHits().getHits();
		assertThat(list.size(), is(5));
		list = articleOperations.search(index, "title:test", 0, 5).getHits().getHits();
		assertThat(list.size(), is(5));
		list = articleOperations.search(index, "content", 0, 20).getHits().getHits();
		assertThat(list.size(), is(20));
		list = articleOperations.search(index, "content", 10, 20).getHits().getHits();
		assertThat(list.size(), is(10));

		List<AggregationBucket> buckets = articleOperations.aggregate(index, TermsAggregation.of("title.keyword"));
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

		buckets = articleOperations.aggregate(index,
				DateHistogramAggregation.of("createdAt", "calendar_interval", "1d", "yyyy-MM-dd"));
		assertThat(buckets.size(), is(20));
		assertThat(buckets.get(0).getKeyAsString(), is("2020-05-01"));
		assertThat(buckets.get(0).getCount(), is(1));
		assertThat(buckets.get(19).getKeyAsString(), is("2020-05-20"));
		assertThat(buckets.get(19).getCount(), is(1));

		articleOperations.delete(index);
	}

	@Test
	public void testScroll() {
		String index = "article";
		if (articleOperations.exists(index))
			articleOperations.delete(index);
		articleOperations.create(index);
		for (int i = 0; i < 10; i++) {
			Article article = new Article(String.valueOf(i + 1), "title", "content", i,
					DateUtils.parse("2020-05-" + NumberUtils.format(i + 1, 2)));
			articleOperations.index(index, article.getId(), article);
		}
		try {
			TimeUnit.MILLISECONDS.sleep(1000); // wait for index completion
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		SearchResult<Article> result = articleOperations.search(index, "title:title", "100s", 3);
		assertThat(result.isTimedOut(), is(false));
		assertThat(result.getHits().getTotal().getValue(), is(10L));
		assertThat(result.getHits().getHits().size(), is(3));
		result = articleOperations.scroll("100s", result.getScrollId());
		assertThat(result.getHits().getHits().size(), is(3));
		result = articleOperations.scroll("100s", result.getScrollId());
		assertThat(result.getHits().getHits().size(), is(3));
		result = articleOperations.scroll("100s", result.getScrollId());
		assertThat(result.getHits().getHits().size(), is(1));
		result = articleOperations.scroll("100s", result.getScrollId());
		assertThat(result.getHits().getHits().size(), is(0));
		articleOperations.clearScroll(result.getScrollId());
		articleOperations.delete(index);
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
