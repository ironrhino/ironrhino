package org.ironrhino.core.fs;

import java.net.URI;

import org.ironrhino.core.fs.impl.LocalFileStorage;
import org.junit.BeforeClass;

public class LocalFileStorageTest extends FileStorageTestBase {

	static LocalFileStorage fs;

	@BeforeClass
	public static void init() {
		fs = new LocalFileStorage();
		fs.setUri(URI.create("file:///tmp/fs"));
		fs.afterPropertiesSet();
	}

	@Override
	public FileStorage getFileStorage() {
		return fs;
	}

}
