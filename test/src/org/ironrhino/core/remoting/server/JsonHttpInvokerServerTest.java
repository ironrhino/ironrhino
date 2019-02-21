package org.ironrhino.core.remoting.server;

import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "httpInvoker.serializationType=JSON")
public class JsonHttpInvokerServerTest extends JavaHttpInvokerServerTest {

}