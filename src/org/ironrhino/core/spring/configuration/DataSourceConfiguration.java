package org.ironrhino.core.spring.configuration;

import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.jdbc.DatabaseProduct;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.AppInfo.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.ClassUtils;

import com.jolbox.bonecp.BoneCPDataSource;
import com.jolbox.bonecp.ConnectionHandle;
import com.jolbox.bonecp.hooks.AbstractConnectionHook;

@Configuration
@ResourcePresentConditional("resources/spring/applicationContext-hibernate.xml")
public class DataSourceConfiguration {

	@Autowired
	private Logger logger;

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

	@Value("${dataSource.maxActive:500}")
	private int maxConnectionsPerPartition;

	@Value("${dataSource.initialSize:5}")
	private int minConnectionsPerPartition;

	@Value("${dataSource.connectionTimeoutInMs:30000}")
	private int connectionTimeoutInMs;

	@Value("${dataSource.idleMaxAgeInMinutes:30}")
	private int idleMaxAgeInMinutes;

	@Value("${dataSource.maxConnectionAgeInSeconds:14000}")
	private int maxConnectionAgeInSeconds;

	@Value("${dataSource.disableJMX:true}")
	private boolean disableJMX = true;

	@Value("${dataSource.idleConnectionTestPeriodInMinutes:10}")
	private int idleConnectionTestPeriodInMinutes;

	@Value("${dataSource.connectionTestStatement:}")
	private String connectionTestStatement;

	@Value("${dataSource.QueryExecuteTimeLimitInMs:5000}")
	private long queryExecuteTimeLimitInMs;

	// aliases for HikariDataSource

	@Value("${dataSource.maximumPoolSize:}") // maxActive
	private Integer maximumPoolSize;

	@Value("${dataSource.minimumIdle:}") // initialSize
	private Integer minimumIdle;

	@Value("${dataSource.connectionTimeout:}") // connectionTimeoutInMs
	private Integer connectionTimeout;

	@Value("${dataSource.idleTimeout:}") // idleMaxAgeInMinutes
	private Integer idleTimeout;

	@Value("${dataSource.maxLifetime:}") // maxConnectionAgeInSeconds
	private Integer maxLifetime;

	@Value("${dataSource.registerMbeans:}")
	private Boolean registerMbeans;

	@Bean(destroyMethod = "close")
	@Primary
	public DataSource dataSource() {
		if (AppInfo.getStage() == Stage.DEVELOPMENT
				&& StringUtils.isBlank(AppInfo.getApplicationContextProperties().getProperty("jdbc.url"))) {
			boolean available = AddressAvailabilityCondition.check(jdbcUrl, 5000);
			if (!available && ClassUtils.isPresent("org.h2.Driver", getClass().getClassLoader())) {
				String newJdbcUrl = "jdbc:h2:" + AppInfo.getAppHome() + "/db/h2";
				logger.warn("Default jdbcUrl {} is not available, switch to {}", jdbcUrl, newJdbcUrl);
				jdbcUrl = newJdbcUrl;
			}
		}
		DataSource ds;
		if (ClassUtils.isPresent("com.zaxxer.hikari.HikariDataSource", getClass().getClassLoader())) {
			ds = hikariDataSource();
		} else {
			logger.warn("Please add HikariCP.jar to use HikariDataSource instead of BoneCPDataSource");
			ds = boneCPDataSource();
		}
		logger.info("Using {} to connect {}", ds.getClass().getName(), jdbcUrl);
		return ds;
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
		if (maximumPoolSize == null)
			maximumPoolSize = maxConnectionsPerPartition;
		if (minimumIdle == null)
			minimumIdle = minConnectionsPerPartition;
		if (connectionTimeout == null)
			connectionTimeout = connectionTimeoutInMs;
		if (idleTimeout == null)
			idleTimeout = idleMaxAgeInMinutes * 60 * 1000;
		if (maxLifetime == null)
			maxLifetime = maxConnectionAgeInSeconds * 1000;
		if (registerMbeans == null)
			registerMbeans = !disableJMX;
		ds.setMaximumPoolSize(maximumPoolSize);
		ds.setMinimumIdle(minimumIdle);
		ds.setConnectionTimeout(connectionTimeout);
		ds.setIdleTimeout(idleTimeout);
		ds.setMaxLifetime(maxLifetime);
		ds.setRegisterMbeans(registerMbeans);
		return ds;
	}

	@Bean
	@Primary
	public JdbcTemplate jdbcTemplate() {
		return new JdbcTemplate(dataSource());
	}

	@Bean
	@Primary
	public NamedParameterJdbcTemplate namedParameterJdbcTemplate() {
		return new NamedParameterJdbcTemplate(dataSource());
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
