package org.ironrhino.core.fs;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class FileStorageTestBase {

	@Autowired
	protected FileStorage fs;

	@Before
	public void cleanup() {
		// delete(fs, "/");
	}

	@Test
	public void testDirectory() throws IOException {
		assertThat(fs.isDirectory("/test"), is(false));
		assertThat(fs.mkdir("/test"), is(true));
		assertThat(fs.isDirectory("/test"), is(true));
		assertThat(fs.delete("/test"), is(true));
		assertThat(fs.isDirectory("/test/test2"), is(false));
		assertThat(fs.mkdir("/test/test2"), is(true));
		assertThat(fs.isDirectory("/test/test2"), is(true));
		assertThat(fs.delete("/test/test2"), is(true));
		assertThat(fs.delete("/test"), is(true));
	}

	@Test
	public void testFile() throws IOException {
		String text = "test";
		String path = "/test/test2/test.txt";
		String path2 = "/test/test2/test2.txt";
		writeToFile(fs, text, path);
		writeToFile(fs, text, path2);
		assertThat(fs.isDirectory("/test"), is(true));
		assertThat(fs.isDirectory("/test/test2/"), is(true));
		assertThat(fs.open("/test/test2/"), is(nullValue()));
		assertThat(fs.open("/test/test2/notexists.txt"), is(nullValue()));
		try (BufferedReader br = new BufferedReader(new InputStreamReader(fs.open(path), StandardCharsets.UTF_8))) {
			assertThat(br.lines().collect(Collectors.joining("\n")), is(text));
		}
		assertThat(fs.exists("/test/"), is(true));
		assertThat(fs.exists(path), is(true));
		assertThat(fs.delete("/test/test2/"), is(false));
		fs.delete(path);
		assertThat(fs.delete("/test/test2/"), is(false));
		fs.delete(path2);
		fs.delete("/test/test2/");
		fs.delete("/test/");
	}

	@Test
	public void testRenameFile() throws IOException {
		String text = "test";
		String path = "/test/test2/test.txt";
		String path2 = "/test/test2/test2.txt";
		writeToFile(fs, text, path);
		assertThat(fs.rename(path, path2), is(true));
		assertThat(fs.exists(path), is(false));
		assertThat(fs.exists(path2), is(true));
		fs.delete(path2);
		fs.delete("/test/test2/");
		fs.delete("/test/");
	}

	@Test
	public void testListFiles() throws IOException {
		fs.mkdir("/test");
		List<FileInfo> files = fs.listFiles("/");
		assertThat(files.isEmpty(), is(true));
		List<FileInfo> fileList = fs.listFilesAndDirectory("/");
		assertThat(fileList.size() == 1, is(true));
		assertThat(isFile(fileList, "test"), is(false));
		writeToFile(fs, "test", "/test.txt");
		files = fs.listFiles("/");
		assertThat(files.size() == 1, is(true));
		fileList = fs.listFilesAndDirectory("/");
		assertThat(fileList.size() == 2, is(true));
		assertThat(isFile(fileList, "test.txt"), is(true));
		assertThat(isFile(fileList, "test"), is(false));

		fs.mkdir("/test/test2");
		files = fs.listFiles("/test");
		assertThat(files.isEmpty(), is(true));
		fileList = fs.listFilesAndDirectory("/test");
		assertThat(fileList.size() == 1, is(true));
		assertThat(isFile(fileList, "test2"), is(false));
		writeToFile(fs, "test", "/test/test.txt");
		files = fs.listFiles("/test");
		assertThat(files.size() == 1, is(true));
		fileList = fs.listFilesAndDirectory("/test");
		assertThat(fileList.size() == 2, is(true));
		assertThat(isFile(fileList, "test.txt"), is(true));
		assertThat(isFile(fileList, "test2"), is(false));

		fs.delete("/test/test2/");
		fs.delete("/test/test.txt");
		fs.delete("/test.txt");
		fs.delete("/test");
	}

	@Test
	public void testListFilesWithMarker() throws IOException {
		String dir = "/test";

		// prepare
		String text = "test";
		for (int i = 0; i < 5; i++)
			fs.mkdir(dir + "/testdir" + i);
		for (int i = 0; i < 5; i++)
			writeToFile(fs, text, dir + "/test" + i + ".txt");

		int total = 0;
		int limit = 2;
		Paged<FileInfo> paged;
		String marker = null;
		do {
			paged = fs.listFiles(dir, limit, marker);
			marker = paged.getNextMarker();
			int size = paged.getResult().size();
			total += size;
			assertThat(size <= limit, is(true));
		} while (marker != null);
		assertThat(total, is(5));

		total = 0;
		limit = 5;
		do {
			paged = fs.listFilesAndDirectory(dir, limit, marker);
			marker = paged.getNextMarker();
			int size = paged.getResult().size();
			total += size;
			assertThat(size <= limit, is(true));
		} while (marker != null);
		assertThat(total, is(10));

		// cleanup
		for (int i = 0; i < 5; i++)
			fs.delete(dir + "/test" + i + ".txt");
		for (int i = 0; i < 5; i++)
			fs.delete(dir + "/testdir" + i);
		fs.delete(dir);
	}

	private static boolean isFile(List<FileInfo> files, String name) {
		for (FileInfo file : files) {
			if (file.getName().equals(name))
				return file.isFile();
		}
		throw new IllegalArgumentException("file '" + name + "' not found");
	}

	protected static void writeToFile(FileStorage fs, String text, String path) throws IOException {
		byte[] bytes = text.getBytes();
		ByteArrayInputStream is = new ByteArrayInputStream(bytes);
		fs.write(is, path, bytes.length);
	}

	protected static void delete(FileStorage fs, String directory) {
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
				delete(fs, path);
			}
		}
		if (!directory.equals("/"))
			fs.delete(directory);
	}

}
