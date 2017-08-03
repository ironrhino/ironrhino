package org.ironrhino.core.jdbc;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;

import lombok.Getter;
import lombok.Setter;

public class IndividualSettingsJdbcTemplate extends JdbcTemplate {

	@Getter
	@Setter
	private int defaultFetchSize;

	private ThreadLocal<Integer> fetchSizeHolder = new ThreadLocal<Integer>() {

		@Override
		protected Integer initialValue() {
			return getDefaultFetchSize();
		}

	};

	@Getter
	@Setter
	private int defaultMaxRows;

	private ThreadLocal<Integer> maxRowsHolder = new ThreadLocal<Integer>() {

		@Override
		protected Integer initialValue() {
			return getDefaultMaxRows();
		}

	};

	@Getter
	@Setter
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
