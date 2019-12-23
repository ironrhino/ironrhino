package org.ironrhino.core.sequence.simple;

import javax.sql.DataSource;

import lombok.Getter;
import lombok.Setter;

public abstract class AbstractDatabaseSimpleSequence extends AbstractSimpleSequence {

	@Getter
	@Setter
	private DataSource dataSource;

	@Getter
	@Setter
	private String tableName = DEFAULT_TABLE_NAME;

	@Getter
	@Setter
	private int cacheSize = 1;

	protected String getActualSequenceName() {
		return getSequenceName() + "_SEQ";
	}

}
