package org.ironrhino.core.service;

import javax.sql.DataSource;

import org.ironrhino.core.hibernate.SessionFactoryBean;
import org.ironrhino.core.hibernate.SpringIdentifierGeneratorFactory;
import org.ironrhino.core.hibernate.SpringIdentifierGeneratorFactoryInitiator;
import org.ironrhino.core.hibernate.StringIdGenerator;
import org.ironrhino.core.spring.configuration.CommonConfiguration;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.zaxxer.hikari.HikariDataSource;

@Configuration
@EnableTransactionManagement(proxyTargetClass = true)
public class HibernateConfiguration extends CommonConfiguration {

	@Bean
	public DataSource dataSource() {
		HikariDataSource ds = new HikariDataSource();
		ds.setJdbcUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
		return ds;
	}

	@Bean(autowire = Autowire.BY_NAME)
	public SessionFactoryBean sessionFactory() {
		SessionFactoryBean sfb = new SessionFactoryBean();
		sfb.setAnnotatedClasses(Person.class);
		sfb.getHibernateProperties().setProperty("hibernate.hbm2ddl.auto", "create-drop");
		return sfb;
	}

	@Bean(autowire = Autowire.BY_NAME)
	public PlatformTransactionManager transactionManager() {
		return new HibernateTransactionManager();
	}

	@Bean
	public PersistenceExceptionTranslationPostProcessor persistenceExceptionTranslationPostProcessor() {
		return new PersistenceExceptionTranslationPostProcessor();
	}

	@SuppressWarnings("rawtypes")
	@Bean
	public EntityManager entityManager() {
		return new EntityManagerImpl();
	}

	@Bean
	public DeleteChecker deleteChecker() {
		return new DeleteChecker();
	}

	@Bean
	public StringIdGenerator stringIdGenerator() {
		return new StringIdGenerator();
	}

	@Bean
	public SpringIdentifierGeneratorFactory springIdentifierGeneratorFactory() {
		return new SpringIdentifierGeneratorFactory();
	}

	@Bean
	public SpringIdentifierGeneratorFactoryInitiator springIdentifierGeneratorFactoryInitiator() {
		return new SpringIdentifierGeneratorFactoryInitiator();
	}

}
