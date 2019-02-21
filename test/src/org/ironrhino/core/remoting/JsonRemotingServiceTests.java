package org.ironrhino.core.remoting;

import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "httpInvoker.serializationType=JSON")
public class JsonRemotingServiceTests extends JavaRemotingServiceTests {

}