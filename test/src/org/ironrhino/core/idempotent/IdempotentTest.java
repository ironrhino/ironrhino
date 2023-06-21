package org.ironrhino.core.idempotent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.ironrhino.core.service.HibernateConfiguration;
import org.ironrhino.core.spring.configuration.RetryConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { IdempotentTest.Config.class, HibernateConfiguration.class, RetryConfiguration.class })
@TestPropertySource(properties = { "annotatedClasses=org.ironrhino.core.idempotent.TestEntity",
		"hibernate.show_sql=true" })
public class IdempotentTest {

	@Autowired
	TestEntityService testEntityService;

	@Test
	public void test() {
		Request request = new Request();
		request.setSeqNo("123456");
		TestEntity entity = testEntityService.save(request);
		assertNotNull(entity.getId());
		assertEquals(request.getSeqNo(), entity.getSeqNo());

		entity = testEntityService.save(request);
		assertNotNull(entity.getId());
		assertEquals(request.getSeqNo(), entity.getSeqNo());
	}

	static class Config {

		@Bean
		TestEntityService testEntityService() {
			return new IdempotentTestEntityService();
		}

	}

}
