package org.ironrhino.core.jdbc;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.zaxxer.hikari.HikariDataSource;

@Configuration
@EnableTransactionManagement(proxyTargetClass = true)
public class JdbcConfiguration {

	@Bean
	public DataSource dataSource() {
		HikariDataSource ds = new HikariDataSource();
		ds.setJdbcUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
		return ds;
	}

	@Bean
	public PlatformTransactionManager transactionManager() {
		return new DataSourceTransactionManager(dataSource());
	}

	@Bean
	public JdbcRepositoryRegistryPostProcessor jdbcRepositoryRegistryPostProcessor() {
		JdbcRepositoryRegistryPostProcessor obj = new JdbcRepositoryRegistryPostProcessor();
		obj.setPackagesToScan(new String[] { getClass().getPackage().getName() });
		return obj;
	}

	@Bean
	public CustomerPartitioner customerPartitioner() {
		return new CustomerPartitioner();
	}

}
