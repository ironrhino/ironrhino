package org.ironrhino.core.sequence.simple;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class CubridSimpleSequence extends AbstractSequenceSimpleSequence {

	@Override
	protected boolean isSequenceExists(Connection conn, String sequenceName) throws SQLException {
		String sql = "SELECT NAME FROM DB_SERIAL";
		try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
			while (rs.next()) {
				if (sequenceName.equalsIgnoreCase(rs.getString("NAME")))
					return true;
			}
		}
		return false;
	}

	@Override
	protected String getCreateSequenceStatement() {
		return new StringBuilder("CREATE SERIAL ").append(getActualSequenceName()).toString();
	}

	@Override
	protected String getQuerySequenceStatement() {
		return new StringBuilder("SELECT ").append(getActualSequenceName()).append(".NEXT_VALUE").toString();
	}

	@Override
	protected void restartSequence(Connection con, Statement stmt) throws SQLException {
		stmt.execute("ALTER SERIAL " + getActualSequenceName() + " START WITH 1");
	}

}
