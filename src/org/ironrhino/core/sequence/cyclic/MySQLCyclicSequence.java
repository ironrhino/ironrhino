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

public class MySQLCyclicSequence extends AbstractDatabaseCyclicSequence {

	@Override
	public void afterPropertiesSet() {
		Connection con = null;
		Statement stmt = null;
		try {
			con = getDataSource().getConnection();
			con.setAutoCommit(true);
			DatabaseMetaData dbmd = con.getMetaData();
			ResultSet rs = dbmd.getTables(null, null, "%", null);
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
				rs = stmt.executeQuery("SELECT * FROM `" + getTableName() + "`");
				boolean columnExists = false;
				ResultSetMetaData metadata = rs.getMetaData();
				for (int i = 0; i < metadata.getColumnCount(); i++) {
					if (columnName.equalsIgnoreCase(metadata.getColumnName(i + 1))) {
						columnExists = true;
						break;
					}
				}
				rs.close();
				if (!columnExists) {
					stmt.execute("ALTER TABLE `" + getTableName() + "` ADD " + columnName
							+ " INT NOT NULL DEFAULT 0,ADD " + columnName + "_TIMESTAMP BIGINT DEFAULT 0");
					stmt.execute("update `" + getTableName() + "` set " + columnName + "_TIMESTAMP=UNIX_TIMESTAMP()");
				}
			} else {
				stmt.execute("CREATE TABLE `" + getTableName() + "` (" + columnName + " INT NOT NULL DEFAULT 0,"
						+ columnName + "_TIMESTAMP BIGINT) ");
				stmt.execute("INSERT INTO `" + getTableName() + "` VALUES(0,UNIX_TIMESTAMP())");
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
			String columnName = getSequenceName();
			if (isSameCycle(con, stmt)) {
				stmt.executeUpdate("UPDATE `" + getTableName() + "` SET " + columnName + " = LAST_INSERT_ID("
						+ columnName + " + 1)," + columnName + "_TIMESTAMP = UNIX_TIMESTAMP()");

			} else {
				if (getLockService().tryLock(getLockName())) {
					try {
						if (isSameCycle(con, stmt)) {
							stmt.executeUpdate(
									"UPDATE `" + getTableName() + "` SET " + columnName + " = LAST_INSERT_ID("
											+ columnName + " + 1)," + columnName + "_TIMESTAMP = UNIX_TIMESTAMP()");
						} else {
							stmt.executeUpdate("UPDATE `" + getTableName() + "` SET " + columnName
									+ " = LAST_INSERT_ID(1)," + columnName + "_TIMESTAMP = UNIX_TIMESTAMP()");
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
			ResultSet rs = null;
			try {
				rs = stmt.executeQuery("SELECT LAST_INSERT_ID(),UNIX_TIMESTAMP() FROM `" + getTableName() + "`");
				if (!rs.next()) {
					throw new DataAccessResourceFailureException("LAST_INSERT_ID() failed after executing an update");
				}
				next = rs.getInt(1);
				Long current = rs.getLong(2);
				if (current < 10000000000L) // no mills
					current *= 1000;
				currentTimestamp = new Date(current);
			} finally {
				if (rs != null)
					rs.close();
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
		ResultSet rs = null;
		try {
			rs = stmt.executeQuery(
					"SELECT  " + getSequenceName() + "_TIMESTAMP,UNIX_TIMESTAMP() FROM `" + getTableName() + "`");
			rs.next();
			Long last = rs.getLong(1);
			if (last < 10000000000L) // no mills
				last *= 1000;
			return getCycleType().isSameCycle(new Date(last), new Date(rs.getLong(2) * 1000));
		} finally {
			if (rs != null)
				rs.close();
		}
	}

}
