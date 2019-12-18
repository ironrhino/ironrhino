package org.ironrhino.batch;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@ContextConfiguration(locations = "/resources/batch/remoteUpdateMessage.xml")
@TestPropertySource(properties = "batch.updateMessage.partitions=50")
public class RemoteUpdateMessageJobTest extends UpdateMessageJobTestBase {

}