package org.ironrhino.core.sequence.cyclic;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.util.Assert;

public class MySQLCyclicSequence extends AbstractDatabaseCyclicSequence {

	@Override
	public void afterPropertiesSet() {
		Assert.notNull(getLockService());
		try (Connection con = getDataSource().getConnection(); Statement stmt = con.createStatement()) {
			String tableName = getTableName();
			boolean tableExists = false;
			con.setAutoCommit(true);
			DatabaseMetaData dbmd = con.getMetaData();
			try (ResultSet rs = dbmd.getTables(null, null, "%", null)) {
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
				try (ResultSet rs = stmt.executeQuery("SELECT * FROM `" + tableName + "` LIMIT 1")) {
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
					stmt.execute("DROP TABLE `" + tableName + "`");
					stmt.execute("CREATE TABLE `" + tableName
							+ "` (NAME VARCHAR(50) PRIMARY KEY, VALUE INT NOT NULL DEFAULT 0, LAST_UPDATED BIGINT) ");
					try (PreparedStatement ps = con.prepareStatement("INSERT INTO " + tableName + " VALUES(?,?,?)")) {
						for (Map.Entry<String, Object> entry : map.entrySet()) {
							if (!entry.getKey().endsWith("_TIMESTAMP")) {
								Object timestamp = map.get(entry.getKey() + "_TIMESTAMP");
								if (timestamp == null)
									timestamp = 0;
								ps.setString(1, entry.getKey());
								ps.setObject(2, entry.getValue());
								ps.setObject(3, timestamp);
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
						.executeQuery("SELECT NAME FROM `" + tableName + "` WHERE NAME='" + sequenceName + "'")) {
					rowExists = rs.next();
				}
				if (!rowExists) {
					stmt.execute("INSERT INTO `" + tableName + "` VALUES('" + sequenceName + "',0,UNIX_TIMESTAMP())");
				}
			} else {
				stmt.execute("CREATE TABLE `" + tableName
						+ "` (NAME VARCHAR(50) PRIMARY KEY, VALUE INT NOT NULL DEFAULT 0, LAST_UPDATED BIGINT) ");
				stmt.execute("INSERT INTO `" + tableName + "` VALUES('" + sequenceName + "',0,UNIX_TIMESTAMP())");
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
			String sequenceName = getSequenceName();
			if (isSameCycle(con, stmt)) {
				stmt.executeUpdate("UPDATE `" + getTableName()
						+ "` SET VALUE = LAST_INSERT_ID(VALUE + 1),LAST_UPDATED = UNIX_TIMESTAMP() WHERE NAME='"
						+ sequenceName + "'");
			} else {
				if (getLockService().tryLock(getLockName())) {
					try {
						if (isSameCycle(con, stmt)) {
							stmt.executeUpdate("UPDATE `" + getTableName()
									+ "` SET VALUE = LAST_INSERT_ID(VALUE + 1),LAST_UPDATED = UNIX_TIMESTAMP() WHERE NAME='"
									+ sequenceName + "'");
						} else {
							stmt.executeUpdate("UPDATE `" + getTableName()
									+ "` SET VALUE = LAST_INSERT_ID(1),LAST_UPDATED = UNIX_TIMESTAMP() WHERE NAME='"
									+ sequenceName + "'");
						}
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
			int next;
			Date currentTimestamp;
			try (ResultSet rs = stmt.executeQuery("SELECT LAST_INSERT_ID(),LAST_UPDATED FROM `" + getTableName()
					+ "` WHERE NAME='" + sequenceName + "'")) {
				if (!rs.next()) {
					throw new DataAccessResourceFailureException("LAST_INSERT_ID() failed after executing an update");
				}
				next = rs.getInt(1);
				Long current = rs.getLong(2);
				if (current < 10000000000L) // no mills
					current *= 1000;
				currentTimestamp = new Date(current);
			}
			Date cycleStart = getCycleType().getCycleStart(currentTimestamp);
			Date cycleEnd = getCycleType().getCycleEnd(currentTimestamp);
			if (currentTimestamp.getTime() - cycleStart.getTime() < CRITICAL_THRESHOLD_TIME && next > 100
					|| cycleEnd.getTime() - currentTimestamp.getTime() < CRITICAL_THRESHOLD_TIME && next < 5) {
				// timestamp updated but sequence not restarted
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
			return getStringValue(currentTimestamp, getPaddingLength(), next);
		} catch (SQLException ex) {
			throw new DataAccessResourceFailureException("Could not obtain last_insert_id()", ex);
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

	protected boolean isSameCycle(Connection con, Statement stmt) throws SQLException {
		try (ResultSet rs = stmt.executeQuery("SELECT LAST_UPDATED,UNIX_TIMESTAMP() FROM `" + getTableName()
				+ "` WHERE NAME='" + getSequenceName() + "'")) {
			rs.next();
			Long last = rs.getLong(1);
			if (last < 10000000000L) // no mills
				last *= 1000;
			return last == 0 || getCycleType().isSameCycle(new Date(last), new Date(rs.getLong(2) * 1000));
		}
	}

}
