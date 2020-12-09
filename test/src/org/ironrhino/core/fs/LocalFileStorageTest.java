package org.ironrhino.core.fs;

import org.ironrhino.core.fs.impl.LocalFileStorage;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = LocalFileStorageTest.Config.class)
@TestPropertySource(properties = "fileStorage.uri=file:///tmp/fs")
public class LocalFileStorageTest extends FileStorageTestBase {

	@Configuration
	static class Config {

		@Bean
		public FileStorage fileStorage() {
			return new LocalFileStorage();
		}

	}

}
