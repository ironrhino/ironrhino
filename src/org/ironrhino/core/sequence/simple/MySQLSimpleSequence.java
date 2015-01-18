package org.ironrhino.core.sequence.simple;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.dao.DataAccessResourceFailureException;

public class MySQLSimpleSequence extends AbstractDatabaseSimpleSequence {

	private AtomicInteger nextId = new AtomicInteger(0);

	private AtomicInteger maxId = new AtomicInteger(0);

	public MySQLSimpleSequence() {
		setCacheSize(10);
	}

	@Override
	public void afterPropertiesSet() {
		Connection con = null;
		Statement stmt = null;
		try {
			con = getDataSource().getConnection();
			con.setAutoCommit(false);
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
							+ columnName + " INT NOT NULL DEFAULT 0");
					con.commit();
				}
			} else {
				stmt.execute("CREATE TABLE `" + getTableName() + "` ("
						+ columnName + " INT NOT NULL DEFAULT 0) ");
				stmt.execute("INSERT INTO `" + getTableName() + "` VALUES(0)");
				con.commit();
			}
		} catch (SQLException ex) {
			throw new DataAccessResourceFailureException(ex.getMessage(), ex);
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
	public int nextIntValue() {
		int next = 0;
		if (this.maxId.get() <= this.nextId.get()) {
			if (getLockService().tryLock(getLockName())) {
				try {
					Connection con = null;
					Statement stmt = null;
					try {
						con = getDataSource().getConnection();
						con.setAutoCommit(true);
						stmt = con.createStatement();
						String columnName = getSequenceName();
						stmt.executeUpdate("UPDATE " + getTableName() + " SET "
								+ columnName + " = LAST_INSERT_ID("
								+ columnName + " + " + getCacheSize() + ")");
						ResultSet rs = null;
						try {
							rs = stmt.executeQuery("SELECT LAST_INSERT_ID()");
							if (!rs.next()) {
								throw new DataAccessResourceFailureException(
										"LAST_INSERT_ID() failed after executing an update");
							}
							int max = rs.getInt(1);
							next = max - getCacheSize() + 1;
							this.nextId.set(next);
							this.maxId.set(max);
						} finally {
							if (rs != null)
								rs.close();
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
				} finally {
					getLockService().unlock(getLockName());
				}
			} else {
				try {
					Thread.sleep(100);
					return nextIntValue();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} else {
			next = this.nextId.incrementAndGet();
		}
		return next;
	}

	@Override
	public void restart() {
		Connection con = null;
		Statement stmt = null;
		try {
			con = getDataSource().getConnection();
			con.setAutoCommit(false);
			stmt = con.createStatement();
			String columnName = getSequenceName();
			stmt.executeUpdate("UPDATE " + getTableName() + " SET "
					+ columnName + " = 0");
			con.commit();
		} catch (SQLException ex) {
			throw new DataAccessResourceFailureException(ex.getMessage(), ex);
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
