package org.ironrhino.core.elasticsearch.index;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Collections;

import org.ironrhino.core.elasticsearch.index.IndexOperationsTests.Config;
import org.ironrhino.rest.client.RestApiRegistryPostProcessor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ClassUtils;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = Config.class)
public class IndexOperationsTests {

	@Autowired
	private IndexOperations indexOperations;

	@Test
	public void test() {
		String index = "test";
		if (indexOperations.exists(index))
			indexOperations.delete(index);
		assertThat(indexOperations.exists(index), is(false));
		Configuration conf = new Configuration();
		conf.setSettings(new Settings());
		conf.setMappings(new Mappings(Collections.singletonMap("name", new Field("keyword"))));
		indexOperations.create(index, conf);
		assertThat(indexOperations.exists(index), is(true));
		Mappings mappings = indexOperations.getMapping(index);
		assertThat(mappings.getProperties().get("name").getType(), is("keyword"));
		indexOperations.putMapping(index, new Mappings(Collections.singletonMap("name2", new Field("text"))));
		mappings = indexOperations.getMapping(index);
		assertThat(mappings.getProperties().get("name2").getType(), is("text"));
		indexOperations.delete(index);
		assertThat(indexOperations.exists(index), is(false));
	}

	static class Config {
		@Bean
		public static RestApiRegistryPostProcessor restApiRegistryPostProcessor() {
			RestApiRegistryPostProcessor obj = new RestApiRegistryPostProcessor();
			obj.setPackagesToScan(new String[] { ClassUtils.getPackageName(IndexOperations.class) });
			return obj;
		}
	}

}
