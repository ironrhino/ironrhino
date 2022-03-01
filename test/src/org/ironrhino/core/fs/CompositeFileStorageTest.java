package org.ironrhino.core.fs;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(locations = "applicationContext-fs.xml")
public class CompositeFileStorageTest extends FileStorageTestBase {

	@Autowired
	@Qualifier("mainFileStorage")
	private FileStorage mainFileStorage;

	@Autowired
	@Qualifier("fallbackFileStorage")
	private FileStorage fallbackFileStorage;

	@Test
	public void testComposite() throws IOException {
		String text = "test";
		String path = "/test/test2/test.txt";
		String path2 = "/test/test2/test2.txt";
		writeToFile(mainFileStorage, text, path);
		writeToFile(fallbackFileStorage, text, path2);
		assertThat(fs.isDirectory("/test"), is(true));
		assertThat(fs.isDirectory("/test/test2/"), is(true));
		try (BufferedReader br = new BufferedReader(new InputStreamReader(fs.open(path), StandardCharsets.UTF_8))) {
			assertThat(br.lines().collect(Collectors.joining("\n")), is(text));
		}
		try (BufferedReader br = new BufferedReader(new InputStreamReader(fs.open(path2), StandardCharsets.UTF_8))) {
			assertThat(br.lines().collect(Collectors.joining("\n")), is(text));
		}
		List<FileInfo> files = fs.listFiles("/test/test2");
		assertThat(files.size(), is(2));
		mainFileStorage.delete(path);
		mainFileStorage.delete("/test/test2/");
		mainFileStorage.delete("/test/");
		fallbackFileStorage.delete(path2);
		fallbackFileStorage.delete("/test/test2/");
		fallbackFileStorage.delete("/test/");
	}

}
