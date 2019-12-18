package org.ironrhino.batch;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@ContextConfiguration(locations = "/resources/batch/partitionUpdateMessage.xml")
@TestPropertySource(properties = "batch.updateMessage.partitions=100")
public class PartitionUpdateMessageJobTest extends UpdateMessageJobTestBase {

}