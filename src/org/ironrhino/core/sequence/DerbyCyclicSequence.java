package org.ironrhino.core.sequence;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DerbyCyclicSequence extends AbstractSequenceCyclicSequence {

	@Override
	protected String getCreateSequenceStatement() {
		return new StringBuilder("CREATE SEQUENCE ")
				.append(getActualSequenceName())
				.append(" AS BIGINT START WITH 1").toString();
	}

	@Override
	protected String getQuerySequenceStatement() {
		return new StringBuilder("SELECT NEXT VALUE FOR ").append(
				getActualSequenceName()).toString();
	}

	@Override
	protected void restartSequence(Connection con, Statement stmt)
			throws SQLException {
		stmt.execute("DROP SEQUENCE " + getActualSequenceName());
		stmt.execute(getCreateSequenceStatement());
		con.commit();
	}

}
