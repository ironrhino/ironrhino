package org.ironrhino.core.fs;

import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(locations = { "local.xml" })
public class LocalFileStorageTest extends FileStorageTestBase {

}
