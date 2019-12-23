package org.ironrhino.core.sequence.simple;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SqlServerSimpleSequence extends AbstractSequenceSimpleSequence {

	@Override
	protected boolean isSequenceExists(Connection conn, String sequenceName) throws SQLException {
		String sql = "SELECT * FROM SYS.SEQUENCES WHERE OBJECT_ID = OBJECT_ID('" + conn.getSchema() + "." + sequenceName
				+ "')";
		try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
			if (rs.next()) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected String getQuerySequenceStatement() {
		return "SELECT NEXT VALUE FOR " + getActualSequenceName();
	}

}
