package org.ironrhino.core.sequence.cyclic;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class CubridCyclicSequence extends AbstractSequenceCyclicSequence {

	@Override
	protected String getCreateSequenceStatement() {
		return new StringBuilder("CREATE SERIAL ").append(getActualSequenceName()).toString();
	}

	@Override
	protected String getQuerySequenceStatement() {
		return new StringBuilder("SELECT ").append(getActualSequenceName()).append(".NEXT_VALUE,")
				.append(getCurrentTimestamp()).append(",LAST_UPDATED FROM ").append(getTableName())
				.append(" WHERE NAME='").append(getSequenceName()).append("'").toString();
	}

	@Override
	protected void restartSequence(Connection con, Statement stmt) throws SQLException {
		stmt.execute("ALTER SERIAL " + getActualSequenceName() + " START WITH 1");
	}

}
