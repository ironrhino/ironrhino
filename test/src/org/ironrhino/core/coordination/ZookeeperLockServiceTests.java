package org.ironrhino.core.coordination;

import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(locations = { "zookeeper.xml" })
public class ZookeeperLockServiceTests extends LockServiceTestBase {
}
