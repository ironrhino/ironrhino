package org.ironrhino.core.sequence.cyclic;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;

public class OracleCyclicSequence extends AbstractSequenceCyclicSequence {

	@Override
	protected String getQuerySequenceStatement() {
		return new StringBuilder("SELECT ").append(getActualSequenceName()).append(".NEXTVAL,")
				.append(getCurrentTimestamp()).append(",").append(getSequenceName()).append("_TIMESTAMP FROM ")
				.append(getTableName()).toString();
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

	@Override
	public String nextStringValue() throws DataAccessException {
		if (!isCriticalPoint())
			return super.nextStringValue();
		String lockName = getLockName() + "_ora";
		if (getLockService().tryLock(lockName)) {
			try {
				return super.nextStringValue();
			} finally {
				getLockService().unlock(lockName);
			}
		} else {
			try {
				Thread.sleep(CRITICAL_THRESHOLD_TIME);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return super.nextStringValue();
		}
	}

	private boolean isCriticalPoint() throws DataAccessException {
		Connection con = null;
		Statement stmt = null;
		try {
			con = getDataSource().getConnection();
			stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery(queryTimestampStatement);
			try {
				rs.next();
				Date currentTimestamp = rs.getTimestamp(1);
				Date cycleStart = getCycleType().getCycleStart(currentTimestamp);
				Date cycleEnd = getCycleType().getCycleEnd(currentTimestamp);
				return currentTimestamp.getTime() - cycleStart.getTime() < CRITICAL_THRESHOLD_TIME
						|| cycleEnd.getTime() - currentTimestamp.getTime() < CRITICAL_THRESHOLD_TIME;
			} finally {
				rs.close();
			}
		} catch (SQLException ex) {
			throw new DataAccessResourceFailureException("Could not obtain current_timestamp", ex);
		} finally {

			if (stmt != null)
				try {
					stmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			if (con != null)
				try {
					con.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}

		}
	}

}
