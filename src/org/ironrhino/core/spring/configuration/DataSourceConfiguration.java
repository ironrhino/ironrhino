package org.ironrhino.core.spring.configuration;

import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.jdbc.DatabaseProduct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.ClassUtils;

import com.jolbox.bonecp.BoneCPDataSource;
import com.jolbox.bonecp.ConnectionHandle;
import com.jolbox.bonecp.hooks.AbstractConnectionHook;

@Configuration
@ResourcePresentConditional("resources/spring/applicationContext-hibernate.xml")
public class DataSourceConfiguration {

	@Autowired
	private Logger logger;

	@Value("${jdbc.dataSourceClassName:}")
	private String dataSourceClassName;

	@Value("${jdbc.driverClass:}")
	private String driverClass;

	@Value("${jdbc.driverClassName:}")
	private String driverClassName;

	@Value("${jdbc.url:jdbc:mysql:///#{systemProperties['app.name'].replaceAll('-','_')}?createDatabaseIfNotExist=true&autoReconnectForPools=true&useUnicode=true&characterEncoding=UTF-8&useServerPrepStmts=true&tinyInt1isBit=false&useSSL=false}")
	private String jdbcUrl;

	@Value("${jdbc.username:root}")
	private String username;

	@Value("${jdbc.password:}")
	private String password;

	@Value("${dataSource.maxActive:1000}")
	private int maxConnectionsPerPartition;

	@Value("${dataSource.initialSize:5}")
	private int minConnectionsPerPartition;

	@Value("${dataSource.connectionTimeoutInMs:30000}")
	private int connectionTimeoutInMs;

	@Value("${dataSource.idleConnectionTestPeriodInMinutes:10}")
	private int idleConnectionTestPeriodInMinutes;

	@Value("${dataSource.idleMaxAgeInMinutes:30}")
	private int idleMaxAgeInMinutes;

	@Value("${dataSource.maxConnectionAgeInSeconds:14000}")
	private int maxConnectionAgeInSeconds;

	@Value("${dataSource.connectionTestStatement:}")
	private String connectionTestStatement;

	@Value("${dataSource.QueryExecuteTimeLimitInMs:5000}")
	private long queryExecuteTimeLimitInMs;

	@Value("${dataSource.disableJMX:true}")
	private boolean disableJMX = true;

	@Bean(destroyMethod = "close")
	@Primary
	public DataSource dataSource() {
		logger.info("Connecting {}", jdbcUrl);
		String hikariClassName = "com.zaxxer.hikari.HikariDataSource";
		if (hikariClassName.equals(dataSourceClassName)) {
			if (ClassUtils.isPresent(hikariClassName, getClass().getClassLoader())) {
				return hikariDataSource();
			} else {
				logger.warn("Class [{}] not found, please add HikariCP.jar", hikariClassName);
			}
		}
		return boneCPDataSource();
	}

	private DataSource boneCPDataSource() {
		DatabaseProduct databaseProduct = DatabaseProduct.parse(jdbcUrl);
		BoneCPDataSource ds = new BoneCPDataSource();
		if (StringUtils.isNotBlank(driverClassName))
			driverClass = driverClassName;
		if (StringUtils.isNotBlank(driverClass))
			ds.setDriverClass(driverClass);
		else if (databaseProduct != null)
			ds.setDriverClass(databaseProduct.getDefaultDriverClass());
		ds.setJdbcUrl(jdbcUrl);
		ds.setUsername(username);
		ds.setPassword(password);
		ds.setMaxConnectionsPerPartition(maxConnectionsPerPartition);
		ds.setMinConnectionsPerPartition(minConnectionsPerPartition);
		ds.setConnectionTimeoutInMs(connectionTimeoutInMs);
		ds.setIdleConnectionTestPeriodInMinutes(idleConnectionTestPeriodInMinutes);
		ds.setIdleMaxAgeInMinutes(idleMaxAgeInMinutes);
		ds.setMaxConnectionAgeInSeconds(maxConnectionAgeInSeconds);
		ds.setDisableJMX(disableJMX);
		ds.setQueryExecuteTimeLimitInMs(queryExecuteTimeLimitInMs);
		ds.setConnectionHook(new MyConnectionHook());
		if (StringUtils.isBlank(connectionTestStatement) && databaseProduct != null)
			connectionTestStatement = databaseProduct.getValidationQuery();
		ds.setConnectionTestStatement(connectionTestStatement);
		ds.setDisableConnectionTracking(true);
		return ds;
	}

	private DataSource hikariDataSource() {
		DatabaseProduct databaseProduct = DatabaseProduct.parse(jdbcUrl);
		com.zaxxer.hikari.HikariDataSource ds = new com.zaxxer.hikari.HikariDataSource();
		if (StringUtils.isNotBlank(driverClass))
			driverClassName = driverClass;
		if (StringUtils.isNotBlank(driverClassName))
			ds.setDriverClassName(driverClassName);
		else if (databaseProduct != null)
			ds.setDriverClassName(databaseProduct.getDefaultDriverClass());
		ds.setJdbcUrl(jdbcUrl);
		ds.setUsername(username);
		ds.setPassword(password);
		ds.setMaximumPoolSize(maxConnectionsPerPartition);
		ds.setMinimumIdle(minConnectionsPerPartition);
		ds.setConnectionTimeout(connectionTimeoutInMs);
		ds.setIdleTimeout(idleMaxAgeInMinutes * 60 * 1000);
		ds.setMaxLifetime(maxConnectionAgeInSeconds * 1000);
		ds.setRegisterMbeans(!disableJMX);
		return ds;
	}

	@Bean
	@Primary
	public JdbcTemplate jdbcTemplate() {
		return new JdbcTemplate(dataSource());
	}

	protected static class MyConnectionHook extends AbstractConnectionHook {

		private Logger logger = LoggerFactory.getLogger("access-warn");

		@Override
		public void onQueryExecuteTimeLimitExceeded(ConnectionHandle handle, Statement statement, String sql,
				Map<Object, Object> logParams, long timeElapsedInNs) {
			boolean withParams = logParams != null && logParams.size() > 0;
			StringBuilder sb = new StringBuilder(40);
			sb.append(" executed /**/ {} /**/ in {} ms");
			if (withParams)
				sb.append(" with {}");
			logger.warn(sb.toString(), sql, TimeUnit.NANOSECONDS.toMillis(timeElapsedInNs), logParams);
		}
	}

}
