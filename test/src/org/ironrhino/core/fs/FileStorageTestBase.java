package org.ironrhino.core.fs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.ironrhino.core.fs.FileStorage;
import org.junit.Test;

public abstract class FileStorageTestBase {

	public abstract FileStorage getFileStorage();

	@Test
	public void testDirectory() throws IOException {
		FileStorage fs = getFileStorage();
		assertFalse(fs.isDirectory("/test"));
		fs.mkdir("/test");
		assertTrue(fs.isDirectory("/test"));
		fs.delete("/test");
		assertFalse(fs.isDirectory("/test/test2"));
		fs.mkdir("/test/test2");
		assertTrue(fs.isDirectory("/test/test2"));
		fs.delete("/test/test2");
		fs.delete("/test");
	}

	@Test
	public void testFile() throws IOException {
		FileStorage fs = getFileStorage();
		String text = "test";
		String path = "/test/test2/test.txt";
		String path2 = "/test/test2/test2.txt";
		writeToFile(fs, text, path);
		writeToFile(fs, text, path2);
		assertTrue(fs.isDirectory("/test"));
		assertTrue(fs.isDirectory("/test/test2/"));
		List<String> lines = IOUtils.readLines(fs.open(path));
		assertTrue(fs.exists("/test/"));
		assertTrue(fs.exists(path));
		assertEquals(text, lines.get(0));
		assertFalse(fs.delete("/test/test2/"));
		fs.delete(path);
		assertFalse(fs.delete("/test/test2/"));
		fs.delete(path2);
		fs.delete("/test/test2/");
		fs.delete("/test/");
	}

	@Test
	public void testRenmaeFile() throws IOException {
		FileStorage fs = getFileStorage();
		String text = "test";
		String path = "/test/test2/test.txt";
		String path2 = "/test2/test.txt";
		writeToFile(fs, text, path);
		assertTrue(fs.rename(path, path2));
		assertFalse(fs.exists(path));
		assertTrue(fs.exists("/test2/"));
		assertTrue(fs.exists(path2));
		fs.delete(path2);
		fs.delete("/test/test2/");
		fs.delete("/test/");
		fs.delete("/test2/");
	}

	@Test
	public void testListFiles() throws IOException {
		FileStorage fs = getFileStorage();
		fs.mkdir("/test");
		List<String> files = fs.listFiles("/");
		assertTrue(files.isEmpty());
		Map<String, Boolean> map = fs.listFilesAndDirectory("/");
		assertTrue(map.size() == 1);
		assertFalse(map.get("test"));
		writeToFile(fs, "test", "/test.txt");
		files = fs.listFiles("/");
		assertTrue(files.size() == 1);
		map = fs.listFilesAndDirectory("/");
		assertTrue(map.size() == 2);
		assertTrue(map.get("test.txt"));
		assertFalse(map.get("test"));

		fs.mkdir("/test/test2");
		files = fs.listFiles("/test");
		assertTrue(files.isEmpty());
		map = fs.listFilesAndDirectory("/test");
		assertTrue(map.size() == 1);
		assertFalse(map.get("test2"));
		writeToFile(fs, "test", "/test/test.txt");
		files = fs.listFiles("/test");
		assertTrue(files.size() == 1);
		map = fs.listFilesAndDirectory("/test");
		assertTrue(map.size() == 2);
		assertTrue(map.get("test.txt"));
		assertFalse(map.get("test2"));

		fs.delete("/test/test2/");
		fs.delete("/test/test.txt");
		fs.delete("/test.txt");
		fs.delete("/test");
	}

	protected static void writeToFile(FileStorage fs, String text, String path) throws IOException {
		ByteArrayInputStream is = new ByteArrayInputStream(text.getBytes());
		fs.write(is, path);
	}

}
