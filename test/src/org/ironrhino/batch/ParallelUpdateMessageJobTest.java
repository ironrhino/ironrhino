package org.ironrhino.batch;

import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(locations = "/resources/batch/parallelUpdateMessage.xml")
public class ParallelUpdateMessageJobTest extends UpdateMessageJobTestBase {

}