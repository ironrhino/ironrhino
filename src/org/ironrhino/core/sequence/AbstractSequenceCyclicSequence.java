package org.ironrhino.core.sequence;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.JdbcUtils;

public abstract class AbstractSequenceCyclicSequence extends
		AbstractDatabaseCyclicSequence {

	protected String getTimestampColumnType() {
		return "TIMESTAMP";
	}

	protected String getCurrentTimestamp() {
		return "CURRENT_TIMESTAMP";
	}

	protected String getCreateTableStatement() {
		return new StringBuilder("CREATE TABLE ").append(getTableName())
				.append(" (").append(getSequenceName()).append("_TIMESTAMP ")
				.append(getTimestampColumnType()).append(")").toString();
	}

	protected String getAddColumnStatement() {
		return new StringBuilder("ALTER TABLE ").append(getTableName())
				.append(" ADD ").append(getSequenceName())
				.append("_TIMESTAMP ").append(getTimestampColumnType())
				.append(" DEFAULT ").append(getCurrentTimestamp()).toString();
	}

	protected String getInsertStatement() {
		return new StringBuilder("INSERT INTO ").append(getTableName())
				.append(" VALUES(").append(getCurrentTimestamp()).append(")")
				.toString();
	}

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
					if ((columnName + "_TIMESTAMP").equalsIgnoreCase(metadata
							.getColumnName(i + 1))) {
						columnExists = true;
						break;
					}
				}
				JdbcUtils.closeResultSet(rs);
				if (!columnExists) {
					stmt.execute(getAddColumnStatement());
					stmt.execute(getCreateSequenceStatement());
					con.commit();
				}
			} else {
				stmt.execute(getCreateTableStatement());
				stmt.execute(getInsertStatement());
				stmt.execute(getCreateSequenceStatement());
				con.commit();
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
		String lockName = getLockName();
		long nextId = 0;
		Connection con = DataSourceUtils.getConnection(getDataSource());
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = con.createStatement();
			if (!isSameCycle(con, stmt)) {
				if (getLockService().tryLock(lockName)) {
					try {
						stmt.executeUpdate("UPDATE " + getTableName() + " SET "
								+ getSequenceName() + "_TIMESTAMP = "
								+ getCurrentTimestamp());
						con.commit();
						restartSequence(con, stmt);
					} finally {
						getLockService().unlock(lockName);
					}
				} else {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			rs = stmt.executeQuery(getQuerySequenceStatement());
			try {
				rs.next();
				nextId = rs.getLong(1);
			} finally {
				JdbcUtils.closeResultSet(rs);
			}
			con.commit();
		} catch (SQLException ex) {
			throw new DataAccessResourceFailureException(
					"Could not obtain next value of sequence", ex);
		} finally {
			JdbcUtils.closeStatement(stmt);
			DataSourceUtils.releaseConnection(con, getDataSource());
		}
		return getStringValue(thisTimestamp, getPaddingLength(), (int) nextId);
	}

	protected boolean isSameCycle(Connection con, Statement stmt)
			throws SQLException {
		DataSourceUtils.applyTransactionTimeout(stmt, getDataSource());
		ResultSet rs = stmt.executeQuery("SELECT  " + getSequenceName()
				+ "_TIMESTAMP," + getCurrentTimestamp() + " FROM "
				+ getTableName());
		try {
			rs.next();
			lastTimestamp = rs.getTimestamp(1);
			thisTimestamp = rs.getTimestamp(2);
		} finally {
			JdbcUtils.closeResultSet(rs);
		}
		return getCycleType().isSameCycle(lastTimestamp, thisTimestamp);
	}

	protected void restartSequence(Connection con, Statement stmt)
			throws SQLException {
		stmt.execute(getRestartSequenceStatement());
		con.commit();
	}

}
