package org.ironrhino.core.sequence.cyclic;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class CubridCyclicSequence extends AbstractSequenceCyclicSequence {

	@Override
	protected String getCreateSequenceStatement() {
		StringBuilder sb = new StringBuilder("CREATE SERIAL ").append(getActualSequenceName());
		if (getCacheSize() > 1)
			sb.append(" CACHE ").append(getCacheSize());
		return sb.toString();
	}

	@Override
	protected String getQuerySequenceStatement() {
		return "SELECT " + getActualSequenceName() + ".NEXT_VALUE," + getCurrentTimestamp() + ",LAST_UPDATED FROM "
				+ getTableName() + " WHERE NAME='" + getSequenceName() + "'";
	}

	@Override
	protected void restartSequence(Connection con, Statement stmt) throws SQLException {
		stmt.execute("ALTER SERIAL " + getActualSequenceName() + " START WITH 1");
	}

}
