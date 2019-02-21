package org.ironrhino.core.remoting;

import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "httpInvoker.serializationType=FST")
public class FstRemotingServiceTests extends JavaRemotingServiceTests {

}