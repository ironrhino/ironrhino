package org.ironrhino.core.sequence.cyclic;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;

public abstract class AbstractSequenceCyclicSequence extends AbstractDatabaseCyclicSequence {

	protected String querySequenceStatement;

	protected String queryTimestampStatement;

	protected String getTimestampColumnType() {
		return "TIMESTAMP";
	}

	protected String getCurrentTimestamp() {
		return "CURRENT_TIMESTAMP";
	}

	protected String getCreateTableStatement() {
		return new StringBuilder("CREATE TABLE ").append(getTableName()).append(" (").append(getSequenceName())
				.append("_TIMESTAMP ").append(getTimestampColumnType()).append(")").toString();
	}

	protected String getAddColumnStatement() {
		return new StringBuilder("ALTER TABLE ").append(getTableName()).append(" ADD ").append(getSequenceName())
				.append("_TIMESTAMP ").append(getTimestampColumnType()).append(" DEFAULT ")
				.append(getCurrentTimestamp()).toString();
	}

	protected String getInsertStatement() {
		return new StringBuilder("INSERT INTO ").append(getTableName()).append(" VALUES(").append(getCurrentTimestamp())
				.append(")").toString();
	}

	protected abstract String getQuerySequenceStatement();

	protected String getCreateSequenceStatement() {
		StringBuilder sb = new StringBuilder("CREATE SEQUENCE ").append(getActualSequenceName());
		if (getCacheSize() > 1)
			sb.append(" CACHE ").append(getCacheSize());
		return sb.toString();
	}

	protected String getRestartSequenceStatement() {
		return new StringBuilder("ALTER SEQUENCE ").append(getActualSequenceName()).append(" RESTART WITH 1")
				.toString();
	}

	@Override
	public void afterPropertiesSet() {
		querySequenceStatement = getQuerySequenceStatement();
		queryTimestampStatement = new StringBuilder("SELECT  ").append(getCurrentTimestamp()).append(",")
				.append(getSequenceName()).append("_TIMESTAMP").append(" FROM ").append(getTableName()).toString();
		Connection con = null;
		Statement stmt = null;
		try {
			con = getDataSource().getConnection();
			con.setAutoCommit(true);
			DatabaseMetaData dbmd = con.getMetaData();
			ResultSet rs = dbmd.getTables(null, null, "%", new String[] { "TABLE" });
			boolean tableExists = false;
			while (rs.next()) {
				if (getTableName().equalsIgnoreCase(rs.getString(3))) {
					tableExists = true;
					break;
				}
			}
			stmt = con.createStatement();
			String columnName = getSequenceName();
			if (tableExists) {
				rs = stmt.executeQuery("SELECT * FROM " + getTableName());
				boolean columnExists = false;
				ResultSetMetaData metadata = rs.getMetaData();
				for (int i = 0; i < metadata.getColumnCount(); i++) {
					if ((columnName + "_TIMESTAMP").equalsIgnoreCase(metadata.getColumnName(i + 1))) {
						columnExists = true;
						break;
					}
				}
				rs.close();
				if (!columnExists) {
					stmt.execute(getAddColumnStatement());
					stmt.execute(getCreateSequenceStatement());
				}
			} else {
				stmt.execute(getCreateTableStatement());
				stmt.execute(getInsertStatement());
				stmt.execute(getCreateSequenceStatement());
			}
		} catch (SQLException ex) {
			logger.error(ex.getMessage(), ex);
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
	public String nextStringValue() throws DataAccessException {
		return nextStringValue(3);
	}

	protected String nextStringValue(int maxAttempts) throws DataAccessException {
		if (maxAttempts < 1)
			throw new IllegalArgumentException("max attempts reached");
		Connection con = null;
		Statement stmt = null;
		try {
			con = getDataSource().getConnection();
			con.setAutoCommit(true);
			stmt = con.createStatement();
			Result result = queryTimestampWithSequence(con, stmt);
			if (getCycleType().isSameCycle(result.lastTimestamp, result.currentTimestamp)) {
				if (result.isCriticalPoint(getCycleType())) {
					// timestamp updated but sequence not restarted
					if (stmt != null)
						try {
							stmt.close();
							stmt = null;
						} catch (SQLException e) {
							e.printStackTrace();
						}
					try {
						con.close();
						con = null;
					} catch (SQLException e) {
						e.printStackTrace();
					}
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					return nextStringValue(--maxAttempts);
				}
				return getStringValue(result.currentTimestamp, getPaddingLength(), result.nextId);
			} else {
				if (getLockService().tryLock(getLockName())) {
					try {
						result = queryTimestampWithSequence(con, stmt);
						if (!getCycleType().isSameCycle(result.lastTimestamp, result.currentTimestamp)) {
							stmt.executeUpdate("UPDATE " + getTableName() + " SET " + getSequenceName()
									+ "_TIMESTAMP = " + getCurrentTimestamp());
							restartSequence(con, stmt);
							result = queryTimestampWithSequence(con, stmt);
						}
						return getStringValue(result.currentTimestamp, getPaddingLength(), result.nextId);
					} finally {
						getLockService().unlock(getLockName());
					}
				} else {
					if (stmt != null)
						try {
							stmt.close();
							stmt = null;
						} catch (SQLException e) {
							e.printStackTrace();
						}
					try {
						con.close();
						con = null;
					} catch (SQLException e) {
						e.printStackTrace();
					}
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					return nextStringValue(--maxAttempts);
				}
			}
		} catch (SQLException ex) {
			throw new DataAccessResourceFailureException("Could not obtain next value of sequence", ex);
		} finally {
			if (stmt != null)
				try {
					stmt.close();
					stmt = null;
				} catch (SQLException e) {
					e.printStackTrace();
				}
			if (con != null)
				try {
					con.close();
					con = null;
				} catch (SQLException e) {
					e.printStackTrace();
				}
		}
	}

	protected void restartSequence(Connection con, Statement stmt) throws SQLException {
		stmt.execute(getRestartSequenceStatement());
	}

	private Result queryTimestampWithSequence(Connection con, Statement stmt) throws SQLException {
		Result result = new Result();
		ResultSet rs = null;
		try {
			rs = stmt.executeQuery(querySequenceStatement);
			rs.next();
			result.nextId = rs.getInt(1);
			if (rs.getMetaData().getColumnCount() > 1) {
				result.currentTimestamp = rs.getTimestamp(2);
				result.lastTimestamp = rs.getTimestamp(3);
			} else {
				Result temp = queryTimestamp(con, stmt);
				result.currentTimestamp = temp.currentTimestamp;
				result.lastTimestamp = temp.lastTimestamp;
			}
			return result;
		} catch (SQLException ex) {
			if (ex.getSQLState().equals("72000") && ex.getErrorCode() == 8004) { // ORA-08004
				stmt.execute("ALTER SEQUENCE " + getActualSequenceName() + " INCREMENT BY 1 MINVALUE 0");
				return queryTimestampWithSequence(con, stmt);
			}
			throw ex;
		} finally {
			if (rs != null)
				rs.close();
		}
	}

	private Result queryTimestamp(Connection con, Statement stmt) throws SQLException {
		Result result = new Result();
		ResultSet rs = stmt.executeQuery(queryTimestampStatement);
		try {
			rs.next();
			result.currentTimestamp = rs.getTimestamp(1);
			result.lastTimestamp = rs.getTimestamp(2);
			return result;
		} finally {
			rs.close();
		}
	}

	private static class Result {

		int nextId;
		Date currentTimestamp;
		Date lastTimestamp;

		boolean isCriticalPoint(CycleType cycleType) {
			return currentTimestamp.getTime()
					- cycleType.getCycleStart(currentTimestamp).getTime() < CRITICAL_THRESHOLD_TIME && nextId > 100
					|| cycleType.getCycleEnd(currentTimestamp).getTime()
							- currentTimestamp.getTime() < CRITICAL_THRESHOLD_TIME && nextId < 5;
		}

	}
}
