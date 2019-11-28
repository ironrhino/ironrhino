package org.ironrhino.batch;

import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(locations = { "ctx.xml", "/resources/batch/partitionUpdateMessage.xml" })
public class PartitionUpdateMessageJobTest extends UpdateMessageJobTestBase {

}