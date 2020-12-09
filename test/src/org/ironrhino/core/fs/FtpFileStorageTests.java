package org.ironrhino.core.fs;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.ironrhino.core.fs.impl.FtpFileStorage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = FtpFileStorageTests.Config.class)
@TestPropertySource(properties = { "fileStorage.uri=ftp://admin:admin@localhost:2121/temp", "ftp.pool.maxTotal=5",
		"ftp.dataTimeout=30000", "ftp.bufferThreshold=8" })
public class FtpFileStorageTests extends FileStorageTestBase {

	@Test
	public void testConcurrency() throws Exception {

		int concurrency = 50;
		int loop = 20;
		ExecutorService es = Executors.newFixedThreadPool(concurrency);
		CountDownLatch cdl = new CountDownLatch(concurrency);
		AtomicInteger errors = new AtomicInteger();
		for (int i = 0; i < concurrency; i++) {
			final int j = i;
			es.execute(() -> {
				String path = "/test" + j + ".txt";
				try {
					for (int k = 0; k < loop; k++) {
						String text = "test" + j + "-" + k;
						writeToFile(fs, text, path);
						try (BufferedReader br = new BufferedReader(
								new InputStreamReader(fs.open(path), StandardCharsets.UTF_8))) {
							if (!text.equals(br.lines().collect(Collectors.joining("\n"))))
								errors.incrementAndGet();
						}
						fs.delete(path);
					}
				} catch (Exception e) {
					e.printStackTrace();
					errors.incrementAndGet();
				} finally {

					cdl.countDown();
				}
			});
		}
		cdl.await();
		es.shutdown();

		assertThat(errors.intValue(), is(0));
	}

	@Configuration
	static class Config {

		@Bean
		public FileStorage fileStorage() {
			return new FtpFileStorage();
		}

	}

}
