package org.ironrhino.core.sequence.cyclic;

public class SqlServerCyclicSequence extends AbstractSequenceCyclicSequence {

	@Override
	protected String getTimestampColumnType() {
		return "DATETIME";
	}

	@Override
	protected String getQuerySequenceStatement() {
		return "SELECT NEXT VALUE FOR " + getActualSequenceName() + "," + getCurrentTimestamp() + ",LAST_UPDATED FROM "
				+ getTableName() + " WHERE NAME='" + getSequenceName() + "'";
	}

	@Override
	protected String getQueryTimestampForUpdateStatement() {
		return "SELECT " + getCurrentTimestamp() + ",LAST_UPDATED" + " FROM " + getTableName()
				+ " WITH(UPDLOCK,ROWLOCK) WHERE NAME='" + getSequenceName() + "'";
	}

}
