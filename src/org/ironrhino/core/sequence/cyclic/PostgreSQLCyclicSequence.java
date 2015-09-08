package org.ironrhino.core.sequence.cyclic;

public class PostgreSQLCyclicSequence extends AbstractSequenceCyclicSequence {

	@Override
	protected String getQuerySequenceStatement() {
		return new StringBuilder("SELECT NEXTVAL('").append(getActualSequenceName()).append("')").append(",")
				.append(getCurrentTimestamp()).append(",").append(getSequenceName()).append("_TIMESTAMP FROM ")
				.append(getTableName()).toString();
	}

}
