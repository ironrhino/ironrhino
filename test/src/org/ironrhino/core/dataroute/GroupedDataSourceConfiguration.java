package org.ironrhino.core.dataroute;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;

import javax.sql.DataSource;

import org.ironrhino.core.jdbc.JdbcRepositoryRegistryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.util.ClassUtils;

import com.zaxxer.hikari.HikariDataSource;

@Configuration
@EnableTransactionManagement(proxyTargetClass = true)
public class GroupedDataSourceConfiguration {

	private DataSource create(String name) {
		HikariDataSource ds = new HikariDataSource();
		ds.setJdbcUrl("jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1");
		ds.setAutoCommit(false);
		try (Connection c = ds.getConnection(); Statement stmt = c.createStatement()) {
			// stmt.execute("drop table pet if exists");
			stmt.execute("create table pet(name varchar(100) primary key)");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return ds;
	}

	@Bean
	public DataSource masterDataSource() {
		return create("master");
	}

	@Bean
	public DataSource slaveDataSource() {
		return create("slave");
	}

	@Bean
	@Primary
	public DataSource dataSource() {
		GroupedDataSource ds = new GroupedDataSource();
		ds.setMasterName("masterDataSource");
		ds.setReadSlaveNames(Collections.singletonMap("slaveDataSource", 1));
		return ds;
	}

	@Bean
	public PlatformTransactionManager transactionManager(DataSource dataSource) {
		return new DataSourceTransactionManager(dataSource);
	}

	@Bean
	public static JdbcRepositoryRegistryPostProcessor jdbcRepositoryRegistryPostProcessor() {
		JdbcRepositoryRegistryPostProcessor obj = new JdbcRepositoryRegistryPostProcessor();
		obj.setPackagesToScan(new String[] { ClassUtils.getPackageName(GroupedDataSourceConfiguration.class) });
		return obj;
	}

}
