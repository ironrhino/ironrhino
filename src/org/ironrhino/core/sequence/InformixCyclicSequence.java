package org.ironrhino.core.sequence;

public class InformixCyclicSequence extends AbstractSequenceCyclicSequence {

	@Override
	protected String getQuerySequenceStatement() {
		return new StringBuilder("SELECT ").append(getActualSequenceName())
				.append(".NEXTVAL FROM INFORMIX.SYSTABLES WHERE TABID=1")
				.toString();
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
