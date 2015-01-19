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

	private int nextId = 0;

	private int maxId = 0;

	public MySQLCyclicSequence() {
		setCacheSize(10);
	}

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
				rs = stmt.executeQuery("SELECT * FROM " + getTableName());
				boolean columnExists = false;
				ResultSetMetaData metadata = rs.getMetaData();
				for (int i = 0; i < metadata.getColumnCount(); i++) {
					if (columnName.equalsIgnoreCase(metadata
							.getColumnName(i + 1))) {
						columnExists = true;
						break;
					}
				}
				rs.close();
				if (!columnExists) {
					stmt.execute("ALTER TABLE `" + getTableName() + "` ADD "
							+ columnName + " INT NOT NULL DEFAULT 0,ADD "
							+ columnName + "_TIMESTAMP BIGINT DEFAULT 0");
					stmt.execute("update `" + getTableName() + "` set "
							+ columnName + "_TIMESTAMP=UNIX_TIMESTAMP()");
				}
			} else {
				stmt.execute("CREATE TABLE `" + getTableName() + "` ("
						+ columnName + " INT NOT NULL DEFAULT 0," + columnName
						+ "_TIMESTAMP BIGINT) ");
				stmt.execute("INSERT INTO `" + getTableName()
						+ "` VALUES(0,UNIX_TIMESTAMP())");
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
		int next = 0;
		Connection con = null;
		Statement stmt = null;
		try {
			con = getDataSource().getConnection();
			con.setAutoCommit(true);
			stmt = con.createStatement();
			String columnName = getSequenceName();
			Date[] array = getLastAndCurrentTimestamp(con, stmt);
			Date lastTimestamp = array[0];
			Date currentTimestamp = array[1];
			if (getLockService().tryLock(getLockName())) {
				try {
					boolean sameCycle = getCycleType().isSameCycle(
							lastTimestamp, currentTimestamp);
					if (sameCycle && maxId > nextId) {
						next = ++nextId;
					} else {
						if (sameCycle) {
							stmt.executeUpdate("UPDATE " + getTableName()
									+ " SET " + columnName
									+ " = LAST_INSERT_ID(" + columnName + " + "
									+ getCacheSize() + ")," + columnName
									+ "_TIMESTAMP = UNIX_TIMESTAMP()");

						} else {
							stmt.executeUpdate("UPDATE " + getTableName()
									+ " SET " + columnName
									+ " = LAST_INSERT_ID(" + getCacheSize()
									+ ")," + columnName
									+ "_TIMESTAMP = UNIX_TIMESTAMP()");
						}
						ResultSet rs = null;
						try {
							rs = stmt.executeQuery("SELECT LAST_INSERT_ID()");
							if (!rs.next()) {
								throw new DataAccessResourceFailureException(
										"LAST_INSERT_ID() failed after executing an update");
							}
							int max = rs.getInt(1);
							next = max - getCacheSize() + 1;
							nextId = next;
							maxId = max;
						} finally {
							if (rs != null)
								rs.close();
						}
					}
					return getStringValue(currentTimestamp, getPaddingLength(),
							next);
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
				if (con != null)
					try {
						con.close();
						con = null;
					} catch (SQLException e) {
						e.printStackTrace();
					}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				return nextStringValue();
			}
		} catch (SQLException ex) {
			throw new DataAccessResourceFailureException(
					"Could not obtain last_insert_id()", ex);
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

	protected Date[] getLastAndCurrentTimestamp(Connection con, Statement stmt)
			throws SQLException {
		ResultSet rs = stmt.executeQuery("SELECT  " + getSequenceName()
				+ "_TIMESTAMP,UNIX_TIMESTAMP() FROM " + getTableName());
		try {
			rs.next();
			Long last = rs.getLong(1);
			if (last < 10000000000L) // no mills
				last *= 1000;
			Date[] array = new Date[2];
			array[0] = new Date(last);
			array[1] = new Date(rs.getLong(2) * 1000);
			return array;
		} finally {
			rs.close();
		}
	}

}
