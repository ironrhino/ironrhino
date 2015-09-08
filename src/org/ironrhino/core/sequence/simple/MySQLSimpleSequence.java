package org.ironrhino.core.sequence.simple;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import org.springframework.dao.DataAccessResourceFailureException;

public class MySQLSimpleSequence extends AbstractDatabaseSimpleSequence {

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
					if (columnName.equalsIgnoreCase(metadata.getColumnName(i + 1))) {
						columnExists = true;
						break;
					}
				}
				rs.close();
				if (!columnExists) {
					stmt.execute("ALTER TABLE `" + getTableName() + "` ADD " + columnName + " INT NOT NULL DEFAULT 0");
				}
			} else {
				stmt.execute("CREATE TABLE `" + getTableName() + "` (" + columnName + " INT NOT NULL DEFAULT 0) ");
				stmt.execute("INSERT INTO `" + getTableName() + "` VALUES(0)");
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
	public int nextIntValue() {
		Connection con = null;
		Statement stmt = null;
		try {
			con = getDataSource().getConnection();
			con.setAutoCommit(true);
			stmt = con.createStatement();
			String columnName = getSequenceName();
			stmt.executeUpdate(
					"UPDATE " + getTableName() + " SET " + columnName + " = LAST_INSERT_ID(" + columnName + " + 1)");
			ResultSet rs = null;
			try {
				rs = stmt.executeQuery("SELECT LAST_INSERT_ID()");
				if (!rs.next()) {
					throw new DataAccessResourceFailureException("LAST_INSERT_ID() failed after executing an update");
				}
				return rs.getInt(1);
			} finally {
				if (rs != null)
					rs.close();
			}
		} catch (SQLException ex) {
			throw new DataAccessResourceFailureException("Could not obtain last_insert_id()", ex);
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
	public void restart() {
		Connection con = null;
		Statement stmt = null;
		try {
			con = getDataSource().getConnection();
			con.setAutoCommit(true);
			stmt = con.createStatement();
			String columnName = getSequenceName();
			stmt.executeUpdate("UPDATE " + getTableName() + " SET " + columnName + " = 0");
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
