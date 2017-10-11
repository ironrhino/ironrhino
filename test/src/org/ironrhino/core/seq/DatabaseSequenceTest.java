package org.ironrhino.core.seq;

import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(locations = { "db.xml" })
public class DatabaseSequenceTest extends SequenceTestBase {
}
