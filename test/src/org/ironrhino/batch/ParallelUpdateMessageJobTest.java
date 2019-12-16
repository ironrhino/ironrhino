package org.ironrhino.batch;

import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(locations = { "ctx.xml", "/resources/batch/parallelUpdateMessage.xml" })
public class ParallelUpdateMessageJobTest extends UpdateMessageJobTestBase {

}