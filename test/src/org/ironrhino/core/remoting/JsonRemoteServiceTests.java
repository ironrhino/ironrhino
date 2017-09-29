package org.ironrhino.core.remoting;

import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(locations = { "json.xml" })
public class JsonRemoteServiceTests extends RemoteServiceTestsBase {
}
