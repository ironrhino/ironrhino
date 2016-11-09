package org.ironrhino.core.sequence.cyclic;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.util.Assert;

public abstract class AbstractSequenceCyclicSequence extends AbstractDatabaseCyclicSequence {

	static final long CRITICAL_THRESHOLD_TIME = 500;

	protected String querySequenceStatement;

	protected String queryTimestampStatement;

	protected String getTimestampColumnType() {
		return "TIMESTAMP";
	}

	protected String getNameColumnType() {
		return "VARCHAR(50)";
	}

	protected String getCurrentTimestamp() {
		return "CURRENT_TIMESTAMP";
	}

	protected String getCreateTableStatement() {
		return new StringBuilder("CREATE TABLE ").append(getTableName()).append(" (NAME ").append(getNameColumnType())
				.append(" PRIMARY KEY, LAST_UPDATED ").append(getTimestampColumnType()).append(")").toString();
	}

	protected String getInsertStatement() {
		return new StringBuilder("INSERT INTO ").append(getTableName()).append(" VALUES(").append("'")
				.append(getSequenceName()).append("',").append(getCurrentTimestamp()).append(")").toString();
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
		Assert.notNull(getLockService());
		querySequenceStatement = getQuerySequenceStatement();
		queryTimestampStatement = new StringBuilder("SELECT ").append(getCurrentTimestamp()).append(",LAST_UPDATED")
				.append(" FROM ").append(getTableName()).append(" WHERE NAME='").append(getSequenceName()).append("'")
				.toString();
		try (Connection con = getDataSource().getConnection(); Statement stmt = con.createStatement()) {
			String tableName = getTableName();
			boolean tableExists = false;
			con.setAutoCommit(true);
			DatabaseMetaData dbmd = con.getMetaData();
			try (ResultSet rs = dbmd.getTables(null, null, "%", new String[] { "TABLE" })) {
				while (rs.next()) {
					if (tableName.equalsIgnoreCase(rs.getString(3))) {
						tableExists = true;
						break;
					}
				}
			}
			if (tableExists) {
				// upgrade legacy
				Map<String, Object> map = null;
				try (ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName)) {
					ResultSetMetaData rsmd = rs.getMetaData();
					boolean legacy = true;
					for (int i = 0; i < rsmd.getColumnCount(); i++)
						if ("LAST_UPDATED".equalsIgnoreCase(rsmd.getColumnName(i + 1))) {
							legacy = false;
							break;
						}
					if (legacy) {
						map = new LinkedHashMap<>();
						rs.next();
						for (int i = 0; i < rsmd.getColumnCount(); i++)
							map.put(rsmd.getColumnName(i + 1), rs.getObject(i + 1));
					}
				}
				if (map != null) {
					stmt.execute("DROP TABLE " + tableName);
					stmt.execute(getCreateTableStatement());
					try (PreparedStatement ps = con.prepareStatement("INSERT INTO " + tableName + " VALUES(?,?)")) {
						for (Map.Entry<String, Object> entry : map.entrySet()) {
							if (entry.getKey().toUpperCase().endsWith("_TIMESTAMP")) {
								String sequenceName = entry.getKey();
								sequenceName = sequenceName.substring(0, sequenceName.lastIndexOf('_'));
								ps.setString(1, sequenceName);
								ps.setTimestamp(2, ((Timestamp) entry.getValue()));
								ps.addBatch();
							}
						}
						ps.executeBatch();
					}
				}
			}
			String sequenceName = getSequenceName();
			if (tableExists) {
				boolean rowExists = false;
				try (ResultSet rs = stmt
						.executeQuery("SELECT NAME FROM " + tableName + " WHERE NAME='" + sequenceName + "'")) {
					rowExists = rs.next();
				}
				if (!rowExists) {
					stmt.execute(getInsertStatement());
					stmt.execute(getCreateSequenceStatement());
				}
			} else {
				stmt.execute(getCreateTableStatement());
				stmt.execute(getInsertStatement());
				stmt.execute(getCreateSequenceStatement());
			}
		} catch (SQLException ex) {
			logger.error(ex.getMessage(), ex);
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
			if (sameCycle(result) == STATUS_CYCLE_SAME_AND_SAFE) {
				return getStringValue(result.currentTimestamp, getPaddingLength(), result.nextId);
			} else {
				if (getLockService().tryLock(getLockName())) {
					try {
						result = queryTimestampWithSequence(con, stmt);
						if (sameCycle(result) == STATUS_CYCLE_CROSS) {
							stmt.executeUpdate("UPDATE " + getTableName() + " SET LAST_UPDATED = "
									+ getCurrentTimestamp() + " WHERE NAME='" + getSequenceName() + "'");
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
						Thread.sleep(CRITICAL_THRESHOLD_TIME);
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

	private int sameCycle(Result result) {
		boolean sameCycle = getCycleType().isSameCycle(result.lastTimestamp, result.currentTimestamp);
		if (!sameCycle)
			return STATUS_CYCLE_CROSS;
		Date cycleStart = getCycleType().getCycleStart(result.currentTimestamp);
		if (result.currentTimestamp.getTime() - cycleStart.getTime() <= CRITICAL_THRESHOLD_TIME) {
			return STATUS_CYCLE_SAME_AND_CRITICAL;
		}
		return STATUS_CYCLE_SAME_AND_SAFE;
	}

	private Result queryTimestampWithSequence(Connection con, Statement stmt) throws SQLException {
		Result result = new Result();
		try (ResultSet rs = stmt.executeQuery(querySequenceStatement)) {
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
		}
	}

	private Result queryTimestamp(Connection con, Statement stmt) throws SQLException {
		Result result = new Result();
		try (ResultSet rs = stmt.executeQuery(queryTimestampStatement)) {
			rs.next();
			result.currentTimestamp = rs.getTimestamp(1);
			result.lastTimestamp = rs.getTimestamp(2);
			return result;
		}
	}

	private static class Result {
		int nextId;
		Date currentTimestamp;
		Date lastTimestamp;
	}
}
