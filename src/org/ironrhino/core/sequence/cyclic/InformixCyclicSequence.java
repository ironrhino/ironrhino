package org.ironrhino.core.sequence.cyclic;

public class InformixCyclicSequence extends AbstractSequenceCyclicSequence {

	@Override
	protected String getQuerySequenceStatement() {
		return "SELECT " + getActualSequenceName() + ".NEXTVAL," + getCurrentTimestamp() + ",LAST_UPDATED FROM "
				+ getTableName() + " WHERE NAME='" + getSequenceName() + "'";
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
