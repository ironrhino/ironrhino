package org.ironrhino.core.cache;

import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(locations = { "cache2k.xml" })
public class Cache2kCacheManagerTest extends CacheManagerTestBase {

}
