package org.ironrhino.core.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.ironrhino.core.util.ErrorMessage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { HibernateConfiguration.class, DeleteCheckerTest.MyConfiguration.class })
@TestPropertySource(properties = {
		"annotatedClasses=org.ironrhino.core.service.Product,org.ironrhino.core.service.Category",
		"hibernate.show_sql=true" })
@SuppressWarnings({ "rawtypes", "unchecked" })
public class DeleteCheckerTest {

	@Autowired
	private EntityManager entityManager;

	@Before
	public void setup() {
		Category c1 = new Category();
		c1.setName("c1");
		entityManager.save(c1);
		Category c2 = new Category();
		c2.setName("c2");
		entityManager.save(c2);
		Product p1 = new Product();
		p1.setName("p1");
		entityManager.save(p1);
		Product p2 = new Product();
		p2.setName("p2");
		p2.setCategory(c2);
		entityManager.save(p2);
	}

	@After
	public void cleanup() {
		entityManager.executeUpdate("delete from Product p");
		entityManager.executeUpdate("delete from Category c");
	}

	@Test
	public void testDeleteWithoutReference() {
		entityManager.setEntityClass(Category.class);
		Category c1 = (Category) entityManager.findByNaturalId("c1");
		entityManager.delete(c1);
		assertThat(entityManager.countAll(), is(1L));
	}

	@Test(expected = ErrorMessage.class)
	public void testDeleteWithReference() {
		entityManager.setEntityClass(Category.class);
		Category c2 = (Category) entityManager.findByNaturalId("c2");
		entityManager.delete(c2);
	}

	@Configuration
	static class MyConfiguration {

		public DeleteChecker deleteChecker() {
			return new DeleteChecker();
		}

	}
}
