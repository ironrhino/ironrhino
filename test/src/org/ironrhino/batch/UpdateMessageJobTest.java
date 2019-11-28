package org.ironrhino.batch;

import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(locations = { "ctx.xml", "/resources/batch/updateMessage.xml" })
public class UpdateMessageJobTest extends UpdateMessageJobTestBase {

}