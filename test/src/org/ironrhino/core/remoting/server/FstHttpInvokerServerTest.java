package org.ironrhino.core.remoting.server;

import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "httpInvoker.serializationType=FST")
public class FstHttpInvokerServerTest extends HttpInvokerServerTestBase {

}