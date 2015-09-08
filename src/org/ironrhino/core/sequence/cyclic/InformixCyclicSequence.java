package org.ironrhino.core.sequence.cyclic;

public class InformixCyclicSequence extends AbstractSequenceCyclicSequence {

	@Override
	protected String getQuerySequenceStatement() {
		return new StringBuilder("SELECT ").append(getActualSequenceName()).append(".NEXTVAL,")
				.append(getCurrentTimestamp()).append(",").append(getSequenceName()).append("_TIMESTAMP FROM ")
				.append(getTableName()).toString();
	}

	@Override
	protected String getTimestampColumnType() {
		return "DATETIME";
	}

	@Override
	protected String getCurrentTimestamp() {
		return "CURRENT";
	}

}
