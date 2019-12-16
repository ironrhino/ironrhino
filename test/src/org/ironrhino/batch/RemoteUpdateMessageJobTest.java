package org.ironrhino.batch;

import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(locations = { "ctx.xml", "/resources/batch/remoteUpdateMessage.xml" })
public class RemoteUpdateMessageJobTest extends UpdateMessageJobTestBase {

}