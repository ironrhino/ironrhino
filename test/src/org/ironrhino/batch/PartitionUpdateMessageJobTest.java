package org.ironrhino.batch;

import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(locations = "/resources/batch/partitionUpdateMessage.xml")
public class PartitionUpdateMessageJobTest extends UpdateMessageJobTestBase {

}