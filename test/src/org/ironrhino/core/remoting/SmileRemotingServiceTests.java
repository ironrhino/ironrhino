package org.ironrhino.core.remoting;

import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "httpInvoker.serializationType=SMILE")
public class SmileRemotingServiceTests extends JavaRemotingServiceTests {

}