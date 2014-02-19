package org.ironrhino.core.jdbc;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;

public class IndividualSettingsJdbcTemplate extends JdbcTemplate {

	private int defaultFetchSize;

	private ThreadLocal<Integer> fetchSizeHolder = new ThreadLocal<Integer>() {

		@Override
		protected Integer initialValue() {
			return getDefaultFetchSize();
		}

	};

	private int defaultMaxRows;

	private ThreadLocal<Integer> maxRowsHolder = new ThreadLocal<Integer>() {

		@Override
		protected Integer initialValue() {
			return getDefaultMaxRows();
		}

	};

	private int defaultQueryTimeout;

	private ThreadLocal<Integer> queryTimeoutHolder = new ThreadLocal<Integer>() {

		@Override
		protected Integer initialValue() {
			return getDefaultQueryTimeout();
		}

	};

	public IndividualSettingsJdbcTemplate() {
		super();
	}

	public IndividualSettingsJdbcTemplate(DataSource ds) {
		super(ds);
	}

	public int getDefaultFetchSize() {
		return defaultFetchSize;
	}

	public void setDefaultFetchSize(int defaultFetchSize) {
		this.defaultFetchSize = defaultFetchSize;
	}

	@Override
	public int getFetchSize() {
		int value = fetchSizeHolder.get();
		fetchSizeHolder.set(getDefaultFetchSize());
		return value;
	}

	@Override
	public void setFetchSize(int fetchSize) {
		fetchSizeHolder.set(fetchSize);
	}

	public int getDefaultMaxRows() {
		return defaultMaxRows;
	}

	public void setDefaultMaxRows(int defaultMaxRows) {
		this.defaultMaxRows = defaultMaxRows;
	}

	@Override
	public int getMaxRows() {
		int value = maxRowsHolder.get();
		maxRowsHolder.set(getDefaultMaxRows());
		return value;
	}

	@Override
	public void setMaxRows(int fetchSize) {
		maxRowsHolder.set(fetchSize);
	}

	public int getDefaultQueryTimeout() {
		return defaultQueryTimeout;
	}

	public void setDefaultQueryTimeout(int defaultQueryTimeout) {
		this.defaultQueryTimeout = defaultQueryTimeout;
	}

	@Override
	public int getQueryTimeout() {
		int value = queryTimeoutHolder.get();
		queryTimeoutHolder.set(getDefaultQueryTimeout());
		return value;
	}

	@Override
	public void setQueryTimeout(int queryTimeout) {
		queryTimeoutHolder.set(queryTimeout);
	}

}
