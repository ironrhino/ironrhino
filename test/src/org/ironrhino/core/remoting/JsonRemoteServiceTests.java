package org.ironrhino.core.remoting;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = RemotingConfiguration.class)
@TestPropertySource(properties = "httpInvoker.serialization.type=JSON")
public class JsonRemoteServiceTests extends RemoteServiceTestsBase {

	@Test
	@Ignore("https://github.com/FasterXML/jackson-databind/issues/2095")
	public void testEchoListWithArray() {
		assertEquals("test",
				testService.echoListWithArray(Collections.singletonList(new String[] { "test" })).get(0)[0]);
	}

}