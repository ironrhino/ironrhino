package org.ironrhino.core.cache;

import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(locations = { "ehcache.xml" })
public class EhCacheCacheManagerTest extends CacheManagerTestBase {

}
