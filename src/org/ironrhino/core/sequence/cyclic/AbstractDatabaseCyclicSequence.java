package org.ironrhino.core.sequence.cyclic;

import javax.sql.DataSource;

import lombok.Getter;
import lombok.Setter;

public abstract class AbstractDatabaseCyclicSequence extends AbstractCyclicSequence {

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
		return new StringBuilder(getSequenceName()).append("_SEQ").toString();
	}

}
