package org.ironrhino.core.sequence;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.JdbcUtils;

public class MySQLCyclicSequence extends AbstractDatabaseCyclicSequence {

	private AtomicInteger nextId = new AtomicInteger(0);

	private AtomicInteger maxId = new AtomicInteger(0);

	private int cacheSize = 20;

	@Override
	public int getCacheSize() {
		return cacheSize;
	}

	@Override
	public void setCacheSize(int cacheSize) {
		this.cacheSize = cacheSize;
	}

	@Override
	public void afterPropertiesSet() {
		Connection con = DataSourceUtils.getConnection(getDataSource());
		Statement stmt = null;
		try {
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
			DataSourceUtils.applyTransactionTimeout(stmt, getDataSource());
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
				JdbcUtils.closeResultSet(rs);
				if (!columnExists) {
					stmt.execute("ALTER TABLE `" + getTableName() + "` ADD "
							+ columnName + " INT NOT NULL DEFAULT 0,ADD "
							+ columnName
							+ "_TIMESTAMP BIGINT DEFAULT UNIX_TIMESTAMP()");
				}
			} else {
				stmt.execute("CREATE TABLE `" + getTableName() + "` ("
						+ columnName + " INT NOT NULL DEFAULT 0," + columnName
						+ "_TIMESTAMP BIGINT) ");
				stmt.execute("INSERT INTO `" + getTableName()
						+ "` VALUES(0,UNIX_TIMESTAMP())");
			}
		} catch (SQLException ex) {
			throw new DataAccessResourceFailureException(ex.getMessage(), ex);
		} finally {
			JdbcUtils.closeStatement(stmt);
			DataSourceUtils.releaseConnection(con, getDataSource());
		}
	}

	@Override
	public String nextStringValue() throws DataAccessException {
		int next = 0;
		if (this.maxId.get() <= this.nextId.get()) {
			if (getLockService().tryLock(getLockName())) {
				try {
					Connection con = DataSourceUtils
							.getConnection(getDataSource());
					Statement stmt = null;
					try {
						stmt = con.createStatement();
						String columnName = getSequenceName();
						if (isSameCycle(con, stmt)) {
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
						con.commit();
						ResultSet rs = stmt
								.executeQuery("SELECT LAST_INSERT_ID()");
						try {
							if (!rs.next()) {
								throw new DataAccessResourceFailureException(
										"LAST_INSERT_ID() failed after executing an update");
							}
							next = rs.getInt(1) - getCacheSize() + 1;
							this.nextId.set(next);
							this.maxId.set(rs.getInt(1));
						} finally {
							JdbcUtils.closeResultSet(rs);
						}

					} catch (SQLException ex) {
						throw new DataAccessResourceFailureException(
								"Could not obtain last_insert_id()", ex);
					} finally {
						JdbcUtils.closeStatement(stmt);
						DataSourceUtils.releaseConnection(con, getDataSource());
					}
				} finally {
					getLockService().unlock(getLockName());
				}
			} else {
				try {
					Thread.sleep(100);
					return nextStringValue();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} else {
			next = this.nextId.incrementAndGet();
		}
		return getStringValue(thisTimestamp, getPaddingLength(), next);
	}

	protected boolean isSameCycle(Connection con, Statement stmt)
			throws SQLException {
		ResultSet rs = stmt.executeQuery("SELECT  " + getSequenceName()
				+ "_TIMESTAMP,UNIX_TIMESTAMP() FROM " + getTableName());
		try {
			rs.next();
			Long last = rs.getLong(1);
			if (last < 10000000000L) // no mills
				last *= 1000;
			lastTimestamp = new Date(last);
			thisTimestamp = new Date(rs.getLong(2) * 1000);
		} finally {
			JdbcUtils.closeResultSet(rs);
		}
		return getCycleType().isSameCycle(lastTimestamp, thisTimestamp);
	}

}
