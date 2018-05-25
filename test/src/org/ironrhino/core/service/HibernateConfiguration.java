package org.ironrhino.core.service;

import org.ironrhino.core.configuration.DataSourceConfiguration;
import org.ironrhino.core.hibernate.SessionFactoryBean;
import org.ironrhino.core.spring.configuration.CommonConfiguration;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@ComponentScan(basePackageClasses = { SessionFactoryBean.class, EntityManager.class })
@EnableTransactionManagement(proxyTargetClass = true)
@Import(DataSourceConfiguration.class)
public class HibernateConfiguration extends CommonConfiguration {

	@Value("${annotatedClasses}")
	private Class<?>[] annotatedClasses;

	@Bean(autowire = Autowire.BY_NAME)
	public SessionFactoryBean sessionFactory() {
		SessionFactoryBean sfb = new SessionFactoryBean();
		sfb.setAnnotatedClasses(annotatedClasses);
		sfb.getHibernateProperties().setProperty("hibernate.hbm2ddl.auto", "update");
		return sfb;
	}

	@Bean(autowire = Autowire.BY_NAME)
	public PlatformTransactionManager transactionManager() {
		return new HibernateTransactionManager();
	}

}
