package org.ironrhino.core.sequence.cyclic;

public class FirebirdCyclicSequence extends AbstractSequenceCyclicSequence {

	@Override
	protected String getQuerySequenceStatement() {
		return "SELECT NEXT VALUE FOR " + getActualSequenceName() + "," + getCurrentTimestamp() + ",LAST_UPDATED FROM "
				+ getTableName() + " WHERE NAME='" + getSequenceName() + "'";
	}

}
