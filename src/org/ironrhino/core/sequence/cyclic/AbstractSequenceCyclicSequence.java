package org.ironrhino.core.sequence.cyclic;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;

import org.ironrhino.core.util.MaxAttemptsExceededException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;

public abstract class AbstractSequenceCyclicSequence extends AbstractDatabaseCyclicSequence {

	private String querySequenceStatement;

	private String queryTimestampForUpdateStatement;

	private String updateTimestampStatement;

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
				.append(" NOT NULL PRIMARY KEY, LAST_UPDATED ").append(getTimestampColumnType()).append(" NOT NULL)")
				.toString();
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

	protected String getQueryTimestampForUpdateStatement() {
		return new StringBuilder("SELECT ").append(getCurrentTimestamp()).append(",LAST_UPDATED").append(" FROM ")
				.append(getTableName()).append(" WHERE NAME='").append(getSequenceName()).append("' FOR UPDATE")
				.toString();
	}

	protected String getUpdateTimestampStatement() {
		return new StringBuilder("UPDATE ").append(getTableName()).append(" SET LAST_UPDATED = ? WHERE NAME='")
				.append(getSequenceName()).append("' AND LAST_UPDATED < ?").toString();
	}

	@Override
	public void afterPropertiesSet() {
		querySequenceStatement = getQuerySequenceStatement();
		queryTimestampForUpdateStatement = getQueryTimestampForUpdateStatement();
		updateTimestampStatement = getUpdateTimestampStatement();
		try {
			createOrUpgradeTable();
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		}
	}

	protected void createOrUpgradeTable() throws SQLException {
		try (Connection conn = getDataSource().getConnection(); Statement stmt = conn.createStatement()) {
			String tableName = getTableName();
			boolean tableExists = false;
			conn.setAutoCommit(true);
			DatabaseMetaData dbmd = conn.getMetaData();
			String catalog = conn.getCatalog();
			String schema = null;
			try {
				schema = conn.getSchema();
			} catch (Throwable t) {
			}
			for (String table : new LinkedHashSet<>(
					Arrays.asList(tableName.toUpperCase(Locale.ROOT), tableName, tableName.toLowerCase(Locale.ROOT)))) {
				try (ResultSet rs = dbmd.getTables(catalog, schema, table, new String[] { "TABLE" })) {
					if (rs.next()) {
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
					try (PreparedStatement ps = conn.prepareStatement("INSERT INTO " + tableName + " VALUES(?,?)")) {
						for (Map.Entry<String, Object> entry : map.entrySet()) {
							if (entry.getKey().toUpperCase(Locale.ROOT).endsWith("_TIMESTAMP")) {
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
		}
	}

	@Override
	public String nextStringValue() throws DataAccessException {
		try (Connection con = getDataSource().getConnection(); Statement stmt = con.createStatement()) {
			con.setAutoCommit(true);
			CycleType ct = getCycleType();
			int maxAttempts = 3;
			int remainingAttempts = maxAttempts;
			do {
				Result result = queryTimestampWithSequence(con, stmt);
				Date now = result.currentTimestamp;
				if (sameCycle(result)) {
					if (updateLastUpdated(con, now, ct.getCycleStart(ct.skipCycles(now, 1))))
						return getStringValue(now, getPaddingLength(), result.nextId);
				} else {
					con.setAutoCommit(false);
					try {
						result = queryTimestampForUpdate(con, stmt);
						if (!sameCycle(result) && updateLastUpdated(con, now, ct.getCycleStart(now))) {
							restartSequence(con, stmt);
							result = queryTimestampWithSequence(con, stmt);
							return getStringValue(result.currentTimestamp, getPaddingLength(), result.nextId);
						}
						con.commit();
					} catch (Exception e) {
						con.rollback();
						throw new DataAccessResourceFailureException(e.getMessage(), e);
					} finally {
						con.setAutoCommit(true);
					}
				}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} while (--remainingAttempts > 0);
			throw new MaxAttemptsExceededException(maxAttempts);
		} catch (SQLException ex) {
			throw new DataAccessResourceFailureException("Could not obtain next value of sequence", ex);
		}
	}

	protected void restartSequence(Connection con, Statement stmt) throws SQLException {
		stmt.execute(getRestartSequenceStatement());
	}

	private boolean updateLastUpdated(Connection con, Date lastUpdated, Date limit) throws SQLException {
		try (PreparedStatement ps = con.prepareStatement(updateTimestampStatement)) {
			ps.setTimestamp(1, new Timestamp(lastUpdated.getTime()));
			ps.setTimestamp(2, new Timestamp(limit.getTime()));
			return ps.executeUpdate() == 1;
		}
	}

	private boolean sameCycle(Result result) {
		return getCycleType().isSameCycle(result.lastTimestamp, result.currentTimestamp);
	}

	private Result queryTimestampWithSequence(Connection con, Statement stmt) throws SQLException {
		Result result = new Result();
		try (ResultSet rs = stmt.executeQuery(querySequenceStatement)) {
			rs.next();
			result.nextId = rs.getInt(1);
			result.currentTimestamp = rs.getTimestamp(2);
			result.lastTimestamp = rs.getTimestamp(3);
			// keep monotonic incrementing
			if (result.lastTimestamp.after(result.currentTimestamp))
				result.currentTimestamp = result.lastTimestamp;
			return result;
		}
	}

	private Result queryTimestampForUpdate(Connection con, Statement stmt) throws SQLException {
		Result result = new Result();
		try (ResultSet rs = stmt.executeQuery(queryTimestampForUpdateStatement)) {
			rs.next();
			result.currentTimestamp = rs.getTimestamp(1);
			result.lastTimestamp = rs.getTimestamp(2);
			// keep monotonic incrementing
			if (result.lastTimestamp.after(result.currentTimestamp))
				result.currentTimestamp = result.lastTimestamp;
			return result;
		}
	}

	private static class Result {
		int nextId;
		Date currentTimestamp;
		Date lastTimestamp;
	}
}
