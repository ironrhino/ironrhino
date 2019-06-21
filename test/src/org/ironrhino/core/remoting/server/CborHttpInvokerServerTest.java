package org.ironrhino.core.remoting.server;

import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "httpInvoker.serializationType=CBOR")
public class CborHttpInvokerServerTest extends JavaHttpInvokerServerTest {

}