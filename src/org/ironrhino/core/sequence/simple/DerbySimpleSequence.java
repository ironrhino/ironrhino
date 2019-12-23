package org.ironrhino.core.sequence.simple;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DerbySimpleSequence extends AbstractSequenceSimpleSequence {

	@Override
	protected String getCreateSequenceStatement() {
		return "CREATE SEQUENCE " + getActualSequenceName() + " AS BIGINT START WITH 1";
	}

	@Override
	protected String getQuerySequenceStatement() {
		return "SELECT NEXT VALUE FOR " + getActualSequenceName();
	}

	@Override
	protected void restartSequence(Connection con, Statement stmt) throws SQLException {
		stmt.execute("DROP SEQUENCE " + getActualSequenceName());
		stmt.execute(getCreateSequenceStatement());
	}

}
