package org.ironrhino.core.sequence.cyclic;

public class PostgreSQLCyclicSequence extends AbstractSequenceCyclicSequence {

	@Override
	protected String getQuerySequenceStatement() {
		return new StringBuilder("SELECT NEXTVAL('").append(getActualSequenceName()).append("')").append(",")
				.append(getCurrentTimestamp()).append(",LAST_UPDATED FROM ").append(getTableName())
				.append(" WHERE NAME='").append(getSequenceName()).append("'").toString();
	}

}
