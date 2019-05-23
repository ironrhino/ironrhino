package org.ironrhino.core.fs;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.ironrhino.core.fs.impl.LocalFileStorage;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class FileStorageMigrationTest {

	static FileStorage source;

	static FileStorage target;

	@BeforeClass
	public static void setup() {
		LocalFileStorage fs1 = new LocalFileStorage();
		fs1.setUri(URI.create("file:///tmp/fs1"));
		fs1.afterPropertiesSet();
		source = fs1;
		LocalFileStorage fs2 = new LocalFileStorage();
		fs2.setUri(URI.create("file:///tmp/fs2"));
		fs2.afterPropertiesSet();
		target = fs2;
	}

	@AfterClass
	public static void cleanup() {
		try {
			cleanup(source, "/");
			cleanup(target, "/");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testMigration() throws IOException {
		for (int i = 0; i < 10; i++)
			for (int j = 0; j < 10; j++)
				writeToFile(source, "text", "/test" + i + "/test" + j + ".txt");
		verify(source);
		source.migrateTo(target, "/", false);
		verify(source);
		verify(target);
		cleanup(target, "/");
		assertThat(target.listFilesAndDirectory("/").isEmpty(), is(true));
		source.migrateTo(target, "/", true);
		assertThat(source.listFilesAndDirectory("/").isEmpty(), is(true));
		verify(target);
	}

	protected static void verify(FileStorage fs) throws IOException {
		List<FileInfo> list = fs.listFilesAndDirectory("/");
		assertThat(list.size(), is(10));
		for (FileInfo file : list) {
			assertThat(file.isFile(), is(false));
			List<FileInfo> list2 = fs.listFilesAndDirectory("/" + file.getName());
			assertThat(list2.size(), is(10));
			for (FileInfo file2 : list2) {
				assertThat(file2.isFile(), is(true));
			}
		}
	}

	protected static void cleanup(FileStorage fs, String directory) throws IOException {
		if (directory == null)
			directory = "/";
		if (!directory.endsWith("/"))
			directory = directory + "/";
		List<FileInfo> files = fs.listFilesAndDirectory(directory);
		for (FileInfo entry : files) {
			String path = directory + entry.getName();
			if (entry.isFile()) {
				fs.delete(path);
			} else {
				cleanup(target, path);
			}
		}
		fs.delete(directory);
	}

	protected static void writeToFile(FileStorage fs, String text, String path) throws IOException {
		byte[] bytes = text.getBytes();
		ByteArrayInputStream is = new ByteArrayInputStream(bytes);
		fs.write(is, path, bytes.length);
	}

}
