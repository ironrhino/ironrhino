package org.ironrhino.core.remoting.server;

import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "httpInvoker.serializationType=JAVA")
public class JavaHttpInvokerServerTest extends HttpInvokerServerTestBase {

}