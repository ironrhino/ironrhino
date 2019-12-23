package org.ironrhino.core.sequence.cyclic;

public class PostgreSQLCyclicSequence extends AbstractSequenceCyclicSequence {

	@Override
	protected String getQuerySequenceStatement() {
		return "SELECT NEXTVAL('" + getActualSequenceName() + "')" + "," + getCurrentTimestamp() + ",LAST_UPDATED FROM "
				+ getTableName() + " WHERE NAME='" + getSequenceName() + "'";
	}

}
