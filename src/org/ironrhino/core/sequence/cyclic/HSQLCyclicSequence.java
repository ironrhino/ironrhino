package org.ironrhino.core.sequence.cyclic;

public class HSQLCyclicSequence extends AbstractSequenceCyclicSequence {

	@Override
	protected String getCreateSequenceStatement() {
		return "CREATE SEQUENCE " + getActualSequenceName() + " AS BIGINT START WITH 1";
	}

	@Override
	protected String getQuerySequenceStatement() {
		return "SELECT NEXT VALUE FOR " + getActualSequenceName() + "," + getCurrentTimestamp() + ",LAST_UPDATED FROM "
				+ getTableName() + " WHERE NAME='" + getSequenceName() + "'";
	}

}
