package org.ironrhino.core.remoting;

import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(locations = { "java.xml" })
public class JavaRemoteServiceTests extends RemoteServiceTestsBase {
}
