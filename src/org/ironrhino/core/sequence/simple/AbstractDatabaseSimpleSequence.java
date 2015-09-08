package org.ironrhino.core.sequence.simple;

import javax.sql.DataSource;

public abstract class AbstractDatabaseSimpleSequence extends AbstractSimpleSequence {

	private DataSource dataSource;

	private String tableName = DEFAULT_TABLE_NAME;

	private int cacheSize = 1;

	public int getCacheSize() {
		return cacheSize;
	}

	public void setCacheSize(int cacheSize) {
		if (cacheSize > 0)
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

}
