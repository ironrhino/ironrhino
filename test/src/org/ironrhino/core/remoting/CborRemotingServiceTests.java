package org.ironrhino.core.remoting;

import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "httpInvoker.serializationType=CBOR")
public class CborRemotingServiceTests extends JavaRemotingServiceTests {

}