package org.ironrhino.core.sequence.cyclic;

public class SqlServerCyclicSequence extends AbstractSequenceCyclicSequence {

	@Override
	protected String getTimestampColumnType() {
		return "DATETIME";
	}

	@Override
	protected String getQuerySequenceStatement() {
		return new StringBuilder("SELECT NEXT VALUE FOR ").append(getActualSequenceName()).append(",")
				.append(getCurrentTimestamp()).append(",LAST_UPDATED FROM ").append(getTableName())
				.append(" WHERE NAME='").append(getSequenceName()).append("'").toString();
	}

	@Override
	protected String getQueryTimestampForUpdateStatement() {
		return new StringBuilder("SELECT ").append(getCurrentTimestamp()).append(",LAST_UPDATED").append(" FROM ")
				.append(getTableName()).append(" WITH(UPDLOCK,ROWLOCK) WHERE NAME='").append(getSequenceName())
				.append("'").toString();
	}

}
