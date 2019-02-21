package org.ironrhino.core.remoting.server;

import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "httpInvoker.serializationType=SMILE")
public class SmileHttpInvokerServerTest extends JavaHttpInvokerServerTest {

}