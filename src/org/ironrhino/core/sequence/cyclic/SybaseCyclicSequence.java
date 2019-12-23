package org.ironrhino.core.sequence.cyclic;

public class SybaseCyclicSequence extends AbstractSequenceCyclicSequence {

	@Override
	protected String getTimestampColumnType() {
		return "TIMESTAMP";
	}

	@Override
	protected String getCurrentTimestamp() {
		return "GETDATE()";
	}

	@Override
	protected String getQuerySequenceStatement() {
		return "SELECT " + getActualSequenceName() + ".NEXTVAL" + "," + getCurrentTimestamp() + ",LAST_UPDATED FROM "
				+ getTableName() + " WHERE NAME='" + getSequenceName() + "'";
	}

}