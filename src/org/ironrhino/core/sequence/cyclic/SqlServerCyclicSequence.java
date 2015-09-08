package org.ironrhino.core.sequence.cyclic;

public class SqlServerCyclicSequence extends AbstractSequenceCyclicSequence {

	@Override
	protected String getTimestampColumnType() {
		return "DATETIME";
	}

	@Override
	protected String getQuerySequenceStatement() {
		return new StringBuilder("SELECT NEXT VALUE FOR ").append(getActualSequenceName()).append(",")
				.append(getCurrentTimestamp()).append(",").append(getSequenceName()).append("_TIMESTAMP FROM ")
				.append(getTableName()).toString();
	}

}
