package org.ironrhino.core.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import javax.servlet.ServletContext;

import org.hibernate.SessionFactory;
import org.ironrhino.core.service.BaseManager;
import org.ironrhino.core.service.BaseManagerImpl;
import org.ironrhino.core.service.DeleteChecker;
import org.ironrhino.core.service.EntityManager;
import org.ironrhino.core.service.EntityManagerImpl;
import org.ironrhino.core.servlet.MainAppInitializer;
import org.ironrhino.sample.crud.Boss;
import org.ironrhino.sample.crud.Company;
import org.ironrhino.sample.crud.Customer;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

public class ApplicationContextUtilsTest {

	@Test
	public void testGetEntityManager() {
		try (AnnotationConfigWebApplicationContext ctx = new AnnotationConfigWebApplicationContext()) {
			setWebApplicationContext(ctx);
			ctx.register(EntityManagerConfig.class);
			ctx.refresh();

			BaseManager<Company> companyBaseManager = ApplicationContextUtils.getEntityManager(Company.class);
			assertThat(companyBaseManager, is(notNullValue()));
			assertThat(companyBaseManager instanceof EntityManager, is(true));

			BaseManager<Boss> bossManager = ApplicationContextUtils.getEntityManager(Boss.class);
			assertThat(bossManager, is(notNullValue()));
			assertThat(bossManager instanceof BossManagerImpl, is(true));

			BaseManager<Customer> customerManager = ApplicationContextUtils.getEntityManager(Customer.class);
			assertThat(customerManager, is(notNullValue()));
			assertThat(customerManager instanceof PrimaryCustomerManagerImpl, is(true));
		}
	}

	private static void setWebApplicationContext(WebApplicationContext ctx) {
		MainAppInitializer.SERVLET_CONTEXT = mock(ServletContext.class);
		given(MainAppInitializer.SERVLET_CONTEXT
				.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE)).willReturn(ctx);
	}

	@Configuration
	static class EntityManagerConfig {

		@Bean
		public SessionFactory sessionFactory() {
			return mock(SessionFactory.class);
		}

		@Bean
		public DeleteChecker deleteChecker() {
			return mock(DeleteChecker.class);
		}

		@Bean
		public EntityManager<?> entityManager() {
			return new EntityManagerImpl<>();
		}

		@Bean
		public BossManager bossManager() {
			return new BossManagerImpl();
		}

		@Bean
		public CustomerManager customerManager() {
			return new CustomerManagerImpl();
		}

		@Bean
		public CustomerManager primaryCustomerManager() {
			return new PrimaryCustomerManagerImpl();
		}

	}

	interface BossManager extends BaseManager<Boss> {
	}

	static class BossManagerImpl extends BaseManagerImpl<Boss> implements BossManager {
	}

	interface CustomerManager extends BaseManager<Customer> {
	}

	static class CustomerManagerImpl extends BaseManagerImpl<Customer> implements CustomerManager {
	}

	@Primary
	static class PrimaryCustomerManagerImpl extends BaseManagerImpl<Customer> implements CustomerManager {
	}
}