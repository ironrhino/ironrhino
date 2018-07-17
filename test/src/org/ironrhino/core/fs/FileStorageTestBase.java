package org.ironrhino.core.fs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
		assertFalse(fs.isDirectory("/test"));
		assertTrue(fs.mkdir("/test"));
		assertTrue(fs.isDirectory("/test"));
		assertTrue(fs.delete("/test"));
		assertFalse(fs.isDirectory("/test/test2"));
		assertTrue(fs.mkdir("/test/test2"));
		assertTrue(fs.isDirectory("/test/test2"));
		assertTrue(fs.delete("/test/test2"));
		assertTrue(fs.delete("/test"));
	}

	@Test
	public void testFile() throws IOException {
		String text = "test";
		String path = "/test/test2/test.txt";
		String path2 = "/test/test2/test2.txt";
		writeToFile(fs, text, path);
		writeToFile(fs, text, path2);
		assertTrue(fs.isDirectory("/test"));
		assertTrue(fs.isDirectory("/test/test2/"));
		assertNull(fs.open("/test/test2/"));
		assertNull(fs.open("/test/test2/notexists.txt"));
		try (BufferedReader br = new BufferedReader(new InputStreamReader(fs.open(path), StandardCharsets.UTF_8))) {
			assertEquals(text, br.lines().collect(Collectors.joining("\n")));
		}
		assertTrue(fs.exists("/test/"));
		assertTrue(fs.exists(path));
		assertFalse(fs.delete("/test/test2/"));
		fs.delete(path);
		assertFalse(fs.delete("/test/test2/"));
		fs.delete(path2);
		fs.delete("/test/test2/");
		fs.delete("/test/");
	}

	@Test
	public void testRenmaeFile() throws IOException {
		String text = "test";
		String path = "/test/test2/test.txt";
		String path2 = "/test/test2/test2.txt";
		writeToFile(fs, text, path);
		assertTrue(fs.rename(path, path2));
		assertFalse(fs.exists(path));
		assertTrue(fs.exists(path2));
		fs.delete(path2);
		fs.delete("/test/test2/");
		fs.delete("/test/");
	}

	@Test
	public void testListFiles() throws IOException {
		fs.mkdir("/test");
		List<FileInfo> files = fs.listFiles("/");
		assertTrue(files.isEmpty());
		List<FileInfo> fileList = fs.listFilesAndDirectory("/");
		assertTrue(fileList.size() == 1);
		assertFalse(isFile(fileList, "test"));
		writeToFile(fs, "test", "/test.txt");
		files = fs.listFiles("/");
		assertTrue(files.size() == 1);
		fileList = fs.listFilesAndDirectory("/");
		assertTrue(fileList.size() == 2);
		assertTrue(isFile(fileList, "test.txt"));
		assertFalse(isFile(fileList, "test"));

		fs.mkdir("/test/test2");
		files = fs.listFiles("/test");
		assertTrue(files.isEmpty());
		fileList = fs.listFilesAndDirectory("/test");
		assertTrue(fileList.size() == 1);
		assertFalse(isFile(fileList, "test2"));
		writeToFile(fs, "test", "/test/test.txt");
		files = fs.listFiles("/test");
		assertTrue(files.size() == 1);
		fileList = fs.listFilesAndDirectory("/test");
		assertTrue(fileList.size() == 2);
		assertTrue(isFile(fileList, "test.txt"));
		assertFalse(isFile(fileList, "test2"));

		fs.delete("/test/test2/");
		fs.delete("/test/test.txt");
		fs.delete("/test.txt");
		fs.delete("/test");
	}

	@Test
	public void testListFilesWithMarker() throws IOException {
		String text = "test";
		for (int i = 0; i < 5; i++)
			fs.mkdir("/test/testdir" + i);
		for (int i = 0; i < 5; i++)
			writeToFile(fs, text, "/test/test" + i + ".txt");
		Paged<FileInfo> paged = fs.listFiles("/test", 2, null);
		assertNull(paged.getMarker());
		assertNotNull(paged.getNextMarker());
		assertEquals(2, paged.getResult().size());
		paged = fs.listFiles("/test", 2, paged.getNextMarker());
		assertNotNull(paged.getMarker());
		assertNotNull(paged.getNextMarker());
		assertEquals(2, paged.getResult().size());
		paged = fs.listFiles("/test", 2, paged.getNextMarker());
		assertNotNull(paged.getMarker());
		// assertNull(paged.getNextMarker());
		assertEquals(1, paged.getResult().size());
		Paged<FileInfo> paged2 = fs.listFilesAndDirectory("/test", 5, null);
		assertNull(paged2.getMarker());
		assertNotNull(paged2.getNextMarker());
		assertEquals(5, paged2.getResult().size());
		paged2 = fs.listFilesAndDirectory("/test", 5, paged2.getNextMarker());
		assertNotNull(paged2.getMarker());
		assertNull(paged2.getNextMarker());
		assertEquals(5, paged2.getResult().size());
		for (int i = 0; i < 5; i++)
			fs.delete("/test/test" + i + ".txt");
		for (int i = 0; i < 5; i++)
			fs.delete("/test/testdir" + i);
		fs.delete("/test");
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
