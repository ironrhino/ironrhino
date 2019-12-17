package org.ironrhino.batch;

import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(locations = "/resources/batch/remoteUpdateMessage.xml")
public class RemoteUpdateMessageJobTest extends UpdateMessageJobTestBase {

}