package org.ironrhino.core.spring.configuration;

import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.jdbc.DatabaseProduct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import com.jolbox.bonecp.BoneCPDataSource;
import com.jolbox.bonecp.ConnectionHandle;
import com.jolbox.bonecp.hooks.AbstractConnectionHook;

@Configuration
@ResourcePresentConditional("resources/spring/applicationContext-hibernate.xml")
public class DataSourceConfiguration {

	@Value("${jdbc.driverClassName:}")
	private String driverClass;

	@Value("${jdbc.url:jdbc:mysql:///${app.name}?createDatabaseIfNotExist=true&autoReconnectForPools=true&useUnicode=true&characterEncoding=UTF-8&useServerPrepStmts=true&tinyInt1isBit=false&rewriteBatchedStatements=true}")
	private String jdbcUrl;

	@Value("${jdbc.username:root}")
	private String username;

	@Value("${jdbc.password:}")
	private String password;

	@Value("${dataSource.maxActive:50}")
	private int maxConnectionsPerPartition;

	@Value("${dataSource.initialSize:5}")
	private int minConnectionsPerPartition;

	@Value("${dataSource.statementsCacheSize:10}")
	private int statementsCacheSize;

	@Value("${dataSource.idleConnectionTestPeriodInMinutes:10}")
	private int idleConnectionTestPeriodInMinutes;

	@Value("${dataSource.idleMaxAgeInMinutes:30}")
	private int idleMaxAgeInMinutes;

	@Value("${dataSource.maxConnectionAgeInSeconds:14400}")
	private int maxConnectionAgeInSeconds;

	@Value("${jdbc.connectionTestStatement:}")
	private String connectionTestStatement;

	@Value("${jdbc.QueryExecuteTimeLimitInMs:5000}")
	private long queryExecuteTimeLimitInMs;

	public @Bean(destroyMethod = "close")
	DataSource dataSource() {
		BoneCPDataSource ds = new BoneCPDataSource();
		if (StringUtils.isNotBlank(driverClass))
			ds.setDriverClass(driverClass);
		ds.setJdbcUrl(jdbcUrl);
		ds.setUsername(username);
		ds.setPassword(password);
		ds.setMaxConnectionsPerPartition(maxConnectionsPerPartition);
		ds.setMinConnectionsPerPartition(minConnectionsPerPartition);
		ds.setStatementsCacheSize(statementsCacheSize);
		ds.setIdleConnectionTestPeriodInMinutes(idleConnectionTestPeriodInMinutes);
		ds.setIdleMaxAgeInMinutes(idleMaxAgeInMinutes);
		ds.setMaxConnectionAgeInSeconds(maxConnectionAgeInSeconds);
		ds.setConnectionHook(new MyConnectionHook());
		ds.setQueryExecuteTimeLimitInMs(queryExecuteTimeLimitInMs);
		DatabaseProduct databaseProduct = DatabaseProduct.parse(jdbcUrl);
		if (StringUtils.isBlank(connectionTestStatement)
				&& databaseProduct != null)
			connectionTestStatement = databaseProduct.getValidationQuery();
		ds.setConnectionTestStatement(connectionTestStatement);
		return ds;
	}

	public @Bean
	JdbcTemplate jdbcTemplate() {
		return new JdbcTemplate(dataSource());
	}

	protected static class MyConnectionHook extends AbstractConnectionHook {

		private Logger logger = LoggerFactory.getLogger("access-warn");

		public void onQueryExecuteTimeLimitExceeded(ConnectionHandle handle,
				Statement statement, String sql, Map<Object, Object> logParams,
				long timeElapsedInNs) {
			boolean withParams = logParams != null && logParams.size() > 0;
			StringBuilder sb = new StringBuilder(40);
			sb.append(" executed /**/ {} /**/ in {} ms");
			if (withParams)
				sb.append(" with {}");
			logger.warn(sb.toString(), sql,
					TimeUnit.NANOSECONDS.toMillis(timeElapsedInNs), logParams);
		}
	}

}
