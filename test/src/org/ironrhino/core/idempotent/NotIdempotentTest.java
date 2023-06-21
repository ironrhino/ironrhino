package org.ironrhino.core.idempotent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.ironrhino.core.service.HibernateConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { NotIdempotentTest.Config.class, HibernateConfiguration.class })
@TestPropertySource(properties = { "annotatedClasses=org.ironrhino.core.idempotent.TestEntity",
		"hibernate.show_sql=true" })
public class NotIdempotentTest {

	@Autowired
	TestEntityService testEntityService;

	@Test(expected = DataIntegrityViolationException.class)
	public void test() {
		Request request = new Request();
		request.setSeqNo("123456");
		TestEntity entity = this.testEntityService.save(request);
		assertNotNull(entity.getId());
		assertEquals(request.getSeqNo(), entity.getSeqNo());

		this.testEntityService.save(request);
	}

	static class Config {

		@Bean
		TestEntityService testEntityService() {
			return new TestEntityService();
		}

	}

}
