package org.ironrhino.core.sequence.cyclic;

public class HSQLCyclicSequence extends AbstractSequenceCyclicSequence {

	@Override
	protected String getCreateSequenceStatement() {
		return new StringBuilder("CREATE SEQUENCE ").append(getActualSequenceName()).append(" AS BIGINT START WITH 1")
				.toString();
	}

	@Override
	protected String getQuerySequenceStatement() {
		return new StringBuilder("SELECT NEXT VALUE FOR ").append(getActualSequenceName()).append(",")
				.append(getCurrentTimestamp()).append(",LAST_UPDATED FROM ").append(getTableName())
				.append(" WHERE NAME='").append(getSequenceName()).append("'").toString();
	}

}
