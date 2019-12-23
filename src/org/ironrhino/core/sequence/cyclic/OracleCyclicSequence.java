package org.ironrhino.core.sequence.cyclic;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class OracleCyclicSequence extends AbstractSequenceCyclicSequence {

	@Override
	protected String getNameColumnType() {
		return "VARCHAR2(50)";
	}

	@Override
	protected String getQuerySequenceStatement() {
		return "SELECT " + getActualSequenceName() + ".NEXTVAL," + getCurrentTimestamp() + ",LAST_UPDATED FROM "
				+ getTableName() + " WHERE NAME='" + getSequenceName() + "'";
	}

	@Override
	protected void restartSequence(Connection con, Statement stmt) throws SQLException {
		boolean autoCommit = con.getAutoCommit();
		con.setAutoCommit(false);
		int current;
		try (ResultSet rs = stmt.executeQuery("SELECT " + getActualSequenceName() + ".NEXTVAL FROM DUAL")) {
			rs.next();
			current = rs.getInt(1);
		}
		stmt.execute("ALTER SEQUENCE " + getActualSequenceName() + " INCREMENT BY -" + current + " MINVALUE 0");
		stmt.execute("SELECT " + getActualSequenceName() + ".NEXTVAL FROM DUAL");
		stmt.execute("ALTER SEQUENCE " + getActualSequenceName() + " INCREMENT BY 1 MINVALUE 0");
		con.commit();
		con.setAutoCommit(autoCommit);
	}

}
