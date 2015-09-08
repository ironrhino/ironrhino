package org.ironrhino.core.sequence.cyclic;

public class SybaseCyclicSequence extends AbstractSequenceCyclicSequence {

	@Override
	protected String getTimestampColumnType() {
		return "DATETIME";
	}

	@Override
	protected String getQuerySequenceStatement() {
		return new StringBuilder("SELECT ").append(getActualSequenceName()).append(".NEXTVAL").append(",")
				.append(getCurrentTimestamp()).append(",").append(getSequenceName()).append("_TIMESTAMP FROM ")
				.append(getTableName()).toString();
	}

}