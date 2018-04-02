package org.ironrhino.core.service;

import javax.sql.DataSource;

import org.ironrhino.core.hibernate.SessionFactoryBean;
import org.ironrhino.core.spring.configuration.CommonConfiguration;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.zaxxer.hikari.HikariDataSource;

@Configuration
@ComponentScan(basePackageClasses = { SessionFactoryBean.class, EntityManager.class })
@EnableTransactionManagement(proxyTargetClass = true)
public class HibernateConfiguration extends CommonConfiguration {

	@Value("${annotatedClasses}")
	private Class<?>[] annotatedClasses;

	@Bean
	public DataSource dataSource() {
		HikariDataSource ds = new HikariDataSource();
		ds.setJdbcUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
		return ds;
	}

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
