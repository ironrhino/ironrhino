package org.ironrhino.core.sequence;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class OracleCyclicSequence extends AbstractSequenceCyclicSequence {

	@Override
	protected String getQuerySequenceStatement() {
		return new StringBuilder("SELECT ").append(getActualSequenceName())
				.append(".NEXTVAL FROM DUAL").toString();
	}

	@Override
	protected void restartSequence(Connection con, Statement stmt)
			throws SQLException {
		stmt.execute("DROP SEQUENCE " + getActualSequenceName());
		stmt.execute(getCreateSequenceStatement());
		con.commit();
	}

}
