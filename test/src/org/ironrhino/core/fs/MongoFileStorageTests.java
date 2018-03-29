package org.ironrhino.core.fs;

import org.ironrhino.core.fs.MongoFileStorageTests.MongoFileStorageConfiguration;
import org.ironrhino.core.fs.impl.MongoFileStorage;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoClientFactoryBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.mongodb.MongoClient;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = MongoFileStorageConfiguration.class)
public class MongoFileStorageTests extends FileStorageTestBase {

	@Configuration
	static class MongoFileStorageConfiguration {

		@Bean
		public MongoClientFactoryBean mongoDbFactory() {
			MongoClientFactoryBean mcfb = new MongoClientFactoryBean();
			return mcfb;
		}

		@Bean
		public MongoTemplate mongoTemplate(MongoClient mongoClient) {
			return new MongoTemplate(mongoClient, "files");
		}

		@Bean
		public FileStorage fileStorage() {
			return new MongoFileStorage();
		}

	}
}
