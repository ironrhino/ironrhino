package org.ironrhino.core.sequence.simple;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;

public abstract class AbstractSequenceSimpleSequence extends
		AbstractDatabaseSimpleSequence {

	protected String getQuerySequenceStatement() {
		return new StringBuilder("SELECT NEXTVAL('")
				.append(getActualSequenceName()).append("')").toString();
	}

	protected String getCreateSequenceStatement() {
		StringBuilder sb = new StringBuilder("CREATE SEQUENCE ")
				.append(getActualSequenceName());
		if (getCacheSize() > 1)
			sb.append(" CACHE ").append(getCacheSize());
		return sb.toString();
	}

	protected String getRestartSequenceStatement() {
		return new StringBuilder("ALTER SEQUENCE ")
				.append(getActualSequenceName()).append(" RESTART WITH 1")
				.toString();
	}

	@Override
	public void afterPropertiesSet() {
		Connection con = null;
		Statement stmt = null;
		try {
			con = getDataSource().getConnection();
			stmt = con.createStatement();
			stmt.execute(getCreateSequenceStatement());
			con.commit();
		} catch (SQLException ex) {
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

	@Override
	public int nextIntValue() throws DataAccessException {
		int nextId = 0;
		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			con = getDataSource().getConnection();
			con.setAutoCommit(false);
			stmt = con.createStatement();
			rs = stmt.executeQuery(getQuerySequenceStatement());
			try {
				rs.next();
				nextId = rs.getInt(1);
			} finally {
				rs.close();
			}
			con.commit();
		} catch (SQLException ex) {
			throw new DataAccessResourceFailureException(
					"Could not obtain next value of sequence", ex);
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
		return nextId;
	}

	public void restart() {
		if (getLockService().tryLock(getLockName())) {
			try {
				Connection con = null;
				Statement stmt = null;
				try {
					con = getDataSource().getConnection();
					con.setAutoCommit(false);
					stmt = con.createStatement();
					restartSequence(con, stmt);
					con.commit();
				} catch (SQLException ex) {
					throw new DataAccessResourceFailureException(
							ex.getMessage(), ex);
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
			} finally {
				getLockService().unlock(getLockName());
			}
		}
	}

	protected void restartSequence(Connection con, Statement stmt)
			throws SQLException {
		stmt.execute(getRestartSequenceStatement());
		con.commit();
	}

}
