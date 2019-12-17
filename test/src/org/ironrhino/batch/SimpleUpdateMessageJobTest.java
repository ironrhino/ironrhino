package org.ironrhino.batch;

import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(locations = "/resources/batch/simpleUpdateMessage.xml")
public class SimpleUpdateMessageJobTest extends UpdateMessageJobTestBase {

}