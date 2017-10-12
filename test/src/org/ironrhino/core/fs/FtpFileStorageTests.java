package org.ironrhino.core.fs;

import java.net.URI;

import org.ironrhino.core.fs.impl.FtpFileStorage;
import org.junit.BeforeClass;

public class FtpFileStorageTests extends FileStorageTestBase {

	static FtpFileStorage fs;

	@BeforeClass
	public static void init() {
		fs = new FtpFileStorage();
		fs.setUri(URI.create("ftp://admin:admin@localhost:2121"));
		fs.init();
	}

	@Override
	public FileStorage getFileStorage() {
		return fs;
	}

}
