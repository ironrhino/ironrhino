package org.ironrhino.core.spring.configuration;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.jdbc.DatabaseProduct;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.AppInfo.Stage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.ClassUtils;

import com.zaxxer.hikari.HikariDataSource;

import lombok.extern.slf4j.Slf4j;

@Configuration
@ResourcePresentConditional("resources/spring/applicationContext-hibernate.xml")
@Slf4j
public class DataSourceConfiguration {

	@Value("${jdbc.driverClass:}")
	private String driverClass;

	@Value("${jdbc.driverClassName:}")
	private String driverClassName;

	@Value("${jdbc.url:jdbc:mysql:///#{systemProperties['app.name'].replaceAll('-','_').replaceAll('\\.','_')}}")
	private String jdbcUrl;

	@Value("${jdbc.username:root}")
	private String username;

	@Value("${jdbc.password:}")
	private String password;

	// aliases for HikariDataSource

	@Value("${dataSource.maximumPoolSize:500}") // maxActive
	private int maximumPoolSize;

	@Value("${dataSource.minimumIdle:5}") // initialSize
	private int minimumIdle;

	@Value("${dataSource.connectionTimeout:10000}") // connectionTimeoutInMs
	private long connectionTimeout;

	@Value("${dataSource.idleTimeout:1800000}") // idleMaxAgeInMinutes
	private long idleTimeout;

	@Value("${dataSource.maxLifetime:7200000}") // maxConnectionAgeInSeconds
	private long maxLifetime;

	@Value("${dataSource.autoCommit:false}")
	private boolean autoCommit;

	@Value("${dataSource.registerMbeans:false}") // disableJMX
	private boolean registerMbeans;

	// legacy alias from dbcp and bonecp

	@Value("${dataSource.maxActive:}") // maximumPoolSize
	private Integer maxConnectionsPerPartition;

	@Value("${dataSource.initialSize:}") // minimumIdle
	private Integer minConnectionsPerPartition;

	@Value("${dataSource.connectionTimeoutInMs:}") // connectionTimeout
	private Long connectionTimeoutInMs;

	@Value("${dataSource.idleMaxAgeInMinutes:}") // idleTimeout
	private Integer idleMaxAgeInMinutes;

	@Value("${dataSource.maxConnectionAgeInSeconds:}") // maxLifetime
	private Integer maxConnectionAgeInSeconds;

	@Value("${dataSource.disableJMX:}") // registerMbeans
	private Boolean disableJMX;

	@Bean(destroyMethod = "close")
	@Primary
	public DataSource dataSource() {
		if (AppInfo.getStage() == Stage.DEVELOPMENT
				&& StringUtils.isBlank(AppInfo.getApplicationContextProperties().getProperty("jdbc.url"))) {
			boolean available = AddressAvailabilityCondition.check(jdbcUrl, 5000);
			if (!available && ClassUtils.isPresent("org.h2.Driver", getClass().getClassLoader())) {
				String newJdbcUrl = "jdbc:h2:" + AppInfo.getAppHome() + "/db/h2";
				log.warn("Default jdbcUrl {} is not available, switch to {}", jdbcUrl, newJdbcUrl);
				jdbcUrl = newJdbcUrl;
			}
		}
		DatabaseProduct databaseProduct = DatabaseProduct.parse(jdbcUrl);
		HikariDataSource ds = new HikariDataSource();
		if (StringUtils.isNotBlank(driverClass))
			driverClassName = driverClass;
		if (StringUtils.isNotBlank(driverClassName))
			ds.setDriverClassName(driverClassName);
		else if (databaseProduct != null)
			ds.setDriverClassName(databaseProduct.getDefaultDriverClass());
		ds.setJdbcUrl(databaseProduct != null ? databaseProduct.polishJdbcUrl(jdbcUrl) : jdbcUrl);
		ds.setUsername(username);
		ds.setPassword(password);
		if (maxConnectionsPerPartition != null)
			maximumPoolSize = maxConnectionsPerPartition;
		if (minConnectionsPerPartition != null)
			minimumIdle = minConnectionsPerPartition;
		if (connectionTimeoutInMs != null)
			connectionTimeout = connectionTimeoutInMs;
		if (idleMaxAgeInMinutes != null)
			idleTimeout = idleMaxAgeInMinutes * 60 * 1000;
		if (maxConnectionAgeInSeconds != null)
			maxLifetime = maxConnectionAgeInSeconds * 1000;
		if (disableJMX != null)
			registerMbeans = !disableJMX;
		ds.setMaximumPoolSize(maximumPoolSize);
		ds.setMinimumIdle(minimumIdle);
		ds.setConnectionTimeout(connectionTimeout);
		ds.setIdleTimeout(idleTimeout);
		ds.setMaxLifetime(maxLifetime);
		ds.setAutoCommit(autoCommit);
		ds.setRegisterMbeans(registerMbeans);
		ds.setPoolName("HikariPool-" + AppInfo.getAppName());
		log.info("Using {} to connect {}", ds.getClass().getName(), ds.getJdbcUrl());
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

}
