package org.ironrhino.core.sequence;

import javax.sql.DataSource;

import org.ironrhino.core.coordination.LockService;

public abstract class AbstractDatabaseCyclicSequence extends
		AbstractCyclicSequence {

	private DataSource dataSource;

	private String tableName = "common_sequence";

	private int cacheSize = 1;

	private LockService lockService;

	public int getCacheSize() {
		return cacheSize;
	}

	public void setCacheSize(int cacheSize) {
		this.cacheSize = cacheSize;
	}

	public DataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	protected String getActualSequenceName() {
		return new StringBuilder(getSequenceName()).append("_SEQ").toString();
	}

	public LockService getLockService() {
		return lockService;
	}

	public void setLockService(LockService lockService) {
		this.lockService = lockService;
	}

	public String getLockName() {
		return "SEQLOCK:" + getSequenceName();
	}

}
