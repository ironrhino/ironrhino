package org.ironrhino.core.fs;

import org.ironrhino.core.fs.FtpFileStorageTests.FtpFileStorageConfiguration;
import org.ironrhino.core.fs.impl.FtpFileStorage;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = FtpFileStorageConfiguration.class)
@TestPropertySource(properties = "fileStorage.uri=ftp://admin:admin@localhost:2121/temp")
public class FtpFileStorageTests extends FileStorageTestBase {

	@Configuration
	static class FtpFileStorageConfiguration {

		@Bean
		public FileStorage fileStorage() {
			return new FtpFileStorage();
		}

	}

}
