package org.ironrhino.core.fs;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(locations = { "mongo.xml" })
public class MongoFileStorageTests extends FileStorageTestBase {

	@Autowired
	private FileStorage fs;

	@Override
	public FileStorage getFileStorage() {
		return fs;
	}

}
