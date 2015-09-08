package org.ironrhino.core.sequence.simple;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class OracleSimpleSequence extends AbstractSequenceSimpleSequence {

	@Override
	protected String getQuerySequenceStatement() {
		return new StringBuilder("SELECT ").append(getActualSequenceName()).append(".NEXTVAL FROM DUAL").toString();
	}

	@Override
	protected void restartSequence(Connection con, Statement stmt) throws SQLException {
		boolean autoCommit = con.getAutoCommit();
		con.setAutoCommit(false);
		int current;
		ResultSet rs = null;
		try {
			rs = stmt.executeQuery("SELECT " + getActualSequenceName() + ".NEXTVAL FROM DUAL");
			rs.next();
			current = rs.getInt(1);
		} finally {
			if (rs != null)
				rs.close();
		}
		stmt.execute("ALTER SEQUENCE " + getActualSequenceName() + " INCREMENT BY -" + current + " MINVALUE 0");
		stmt.execute("SELECT " + getActualSequenceName() + ".NEXTVAL FROM DUAL");
		stmt.execute("ALTER SEQUENCE " + getActualSequenceName() + " INCREMENT BY 1 MINVALUE 0");
		con.commit();
		con.setAutoCommit(autoCommit);
	}

}
