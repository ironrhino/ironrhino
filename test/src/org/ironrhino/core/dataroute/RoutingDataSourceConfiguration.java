package org.ironrhino.core.dataroute;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

import javax.sql.DataSource;

import org.ironrhino.core.jdbc.JdbcRepositoryRegistryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.zaxxer.hikari.HikariDataSource;

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableTransactionManagement(proxyTargetClass = true, order = 0)
public class RoutingDataSourceConfiguration {

	private DataSource create(String name) {
		HikariDataSource ds = new HikariDataSource();
		ds.setJdbcUrl("jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1");
		ds.setAutoCommit(false);
		try (Connection c = ds.getConnection(); Statement stmt = c.createStatement()) {
			stmt.execute("create table pet(name varchar(100) primary key)");
			if (name.equals("sharding3"))
				stmt.execute("create table ownership(name varchar(100), owner varchar(100))");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return ds;
	}

	@Bean
	public DataSource sharding0() {
		return create("sharding0");
	}

	@Bean
	public DataSource sharding1() {
		return create("sharding1");
	}

	@Bean
	public DataSource sharding2() {
		return create("sharding2");
	}

	@Bean
	public DataSource sharding3() {
		return create("sharding3");
	}

	@Bean
	public DataSource dataSource() {
		RoutingDataSource ds = new RoutingDataSource();
		ds.setShardingNames(Arrays.asList("sharding0", "sharding1", "sharding2", "sharding3"));
		ds.setDefaultRouter(defaultRouter());
		return ds;
	}

	@Bean
	public Router defaultRouter() {
		return (nodes, key) -> {
			return key.toString().length() % nodes.size();
		};
	}

	@Bean
	public PlatformTransactionManager transactionManager(DataSource dataSource) {
		return new DataSourceTransactionManager(dataSource);
	}

	@Bean
	public JdbcRepositoryRegistryPostProcessor jdbcRepositoryRegistryPostProcessor() {
		JdbcRepositoryRegistryPostProcessor obj = new JdbcRepositoryRegistryPostProcessor();
		obj.setPackagesToScan(new String[] { getClass().getPackage().getName() });
		return obj;
	}

	@Bean
	public DataRouteAspect dataRouteAspect() {
		return new DataRouteAspect();
	}

	@Bean
	public ShardingsTemplateHolder shardingsTemplateHolder(DataSource dataSource) {
		return new ShardingsTemplateHolder(dataSource);
	}

}
