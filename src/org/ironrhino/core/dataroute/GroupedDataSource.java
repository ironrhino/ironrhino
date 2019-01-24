package org.ironrhino.core.dataroute;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.ironrhino.core.util.MaxAttemptsExceededException;
import org.ironrhino.core.util.RoundRobin;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

public class GroupedDataSource extends LazyConnectionDataSourceProxy
		implements InitializingBean, BeanFactoryAware, BeanNameAware {

	// inject starts
	@Getter
	@Setter
	private String masterName;

	@Getter
	@Setter
	private Map<String, Integer> writeSlaveNames;

	@Getter
	@Setter
	private Map<String, Integer> readSlaveNames;

	@Getter
	@Setter
	private int maxAttempts = 3;

	@Getter
	@Setter
	private int deadFailureThreshold = 3;

	// inject end

	@Getter
	private BeanFactory beanFactory;

	@Getter
	private String groupName;

	private InternalGroupedDataSource targetDataSource;

	@Override
	public void setBeanName(String beanName) {
		this.groupName = beanName;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public void afterPropertiesSet() {
		targetDataSource = new InternalGroupedDataSource();
		BeanUtils.copyProperties(this, targetDataSource, "logWriter", "loginTimeout");
		targetDataSource.afterPropertiesSet();
		setTargetDataSource(targetDataSource);
		super.afterPropertiesSet();
	}

	@Scheduled(initialDelayString = "${dataSource.tryRecover.initialDelay:300000}", fixedDelayString = "${dataSource.tryRecover.fixedDelay:300000}")
	public void tryRecover() {
		targetDataSource.tryRecover();
	}

	@Slf4j
	static class InternalGroupedDataSource extends AbstractDataSource {

		@Getter
		@Setter
		private String masterName;

		@Getter
		@Setter
		private Map<String, Integer> readSlaveNames;

		@Getter
		@Setter
		private Map<String, Integer> writeSlaveNames;

		@Getter
		@Setter
		private int maxAttempts = 3;

		@Getter
		@Setter
		private int deadFailureThreshold = 3;

		@Getter
		@Setter
		private BeanFactory beanFactory;

		@Setter
		@Getter
		private String groupName;

		// inject end

		private DataSource master;

		private Map<String, DataSource> readSlaves = new HashMap<>();

		private Map<String, DataSource> writeSlaves = new HashMap<>();

		private RoundRobin<String> readRoundRobin;

		private RoundRobin<String> writeRoundRobin;

		private Set<DataSource> deadDataSources = new HashSet<>();

		private Map<DataSource, Integer> failureCount = new ConcurrentHashMap<>();

		public void afterPropertiesSet() {
			master = (DataSource) beanFactory.getBean(masterName);
			if (readSlaveNames != null && readSlaveNames.size() > 0) {
				for (String name : readSlaveNames.keySet())
					readSlaves.put(name, (DataSource) beanFactory.getBean(name));
				readRoundRobin = new RoundRobin<>(readSlaveNames,
						target -> !deadDataSources.contains(readSlaves.get(target)));
			}
			if (writeSlaveNames != null && writeSlaveNames.size() > 0) {
				for (String name : writeSlaveNames.keySet())
					writeSlaves.put(name, (DataSource) beanFactory.getBean(name));
				writeSlaves.put(masterName, master);
				writeRoundRobin = new RoundRobin<>(writeSlaveNames,
						target -> !deadDataSources.contains(writeSlaves.get(target)));
			}
		}

		@Override
		public Connection getConnection(String username, String password) throws SQLException {
			return getConnection(username, password, maxAttempts);
		}

		public Connection getConnection(String username, String password, int maxAttempts) throws SQLException {
			DataSource ds = null;
			String dbname = null;
			boolean read = false;
			boolean readonly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
			if (readonly && readRoundRobin != null) {
				read = true;
				dbname = readRoundRobin.pick();
				ds = readSlaves.get(dbname);
			}
			if (ds == null && writeRoundRobin != null) {
				dbname = writeRoundRobin.pick();
				ds = writeSlaves.get(dbname);
			}
			if (ds == null && masterName != null) {
				dbname = masterName;
				ds = master;
			}
			if (ds == null)
				throw new IllegalStateException("No underlying DataSource found");
			int remainingAttempts = maxAttempts;
			do {
				try {
					Connection conn = username == null ? ds.getConnection() : ds.getConnection(username, password);
					if (read)
						conn.setReadOnly(true);
					failureCount.remove(ds);
					return conn;
				} catch (SQLException e) {
					log.error(e.getMessage(), e);
					if (remainingAttempts <= 1)
						throw e;
					Integer failureTimes = failureCount.get(ds);
					if (failureTimes == null)
						failureTimes = 1;
					else
						failureTimes += 1;
					if (failureTimes == deadFailureThreshold) {
						failureCount.remove(ds);
						deadDataSources.add(ds);
						log.error("dataSource[" + groupName + ':' + dbname + "] down!");
					} else {
						failureCount.put(ds, failureTimes);
					}
				}
			} while (--remainingAttempts > 0);
			throw new MaxAttemptsExceededException(maxAttempts);
		}

		@Override
		public Connection getConnection() throws SQLException {
			return getConnection(null, null);
		}

		public void tryRecover() {
			Iterator<DataSource> it = deadDataSources.iterator();
			while (it.hasNext()) {
				DataSource ds = it.next();
				try (Connection conn = ds.getConnection()) {
					if (conn.isValid(5)) {
						it.remove();
						String dbname = null;
						for (Map.Entry<String, DataSource> entry : writeSlaves.entrySet()) {
							if (entry.getValue() == ds) {
								dbname = entry.getKey();
								break;
							}
						}
						if (dbname == null)
							for (Map.Entry<String, DataSource> entry : readSlaves.entrySet()) {
								if (entry.getValue() == ds) {
									dbname = entry.getKey();
									break;
								}
							}
						log.warn("dataSource[" + groupName + ':' + dbname + "] recovered");
					}
				} catch (Exception e) {
					log.debug(e.getMessage(), e);
				}
			}
		}

	}

}