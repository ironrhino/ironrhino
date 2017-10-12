package org.ironrhino.core.seq;

import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(locations = { "redis.xml" })
public class RedisSequenceTests extends SequenceTestBase {
}
