package org.ironrhino.core.sequence.simple;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessResourceFailureException;

public class MySQLSimpleSequence extends AbstractDatabaseSimpleSequence {

	private long nextId = 0;

	private long maxId = 0;

	@Override
	public void afterPropertiesSet() {
		try {
			createTable(getDataSource(), getTableName(), getSequenceName());
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public long nextLongValue() {
		int cacheSize = getCacheSize();
		if (cacheSize > 1) {
			synchronized (this) {
				if (maxId == nextId) {
					maxId = incrementAndGet(cacheSize);
					nextId = maxId - cacheSize + 1;
				} else {
					nextId++;
				}
				return nextId;
			}
		}
		return incrementAndGet(1);
	}

	private long incrementAndGet(int increment) {
		try (Connection con = getDataSource().getConnection()) {
			con.setAutoCommit(true);
			try (Statement stmt = con.createStatement()) {
				String sequenceName = getSequenceName();
				stmt.executeUpdate("UPDATE `" + getTableName() + "` SET VALUE = LAST_INSERT_ID(VALUE + " + increment
						+ ") WHERE NAME='" + sequenceName + "'");
				try (ResultSet rs = stmt.executeQuery("SELECT LAST_INSERT_ID()")) {
					if (!rs.next()) {
						throw new DataAccessResourceFailureException(
								"LAST_INSERT_ID() failed after executing an update");
					}
					return rs.getLong(1);
				}
			}
		} catch (SQLException ex) {
			throw new DataAccessResourceFailureException("Could not obtain LAST_INSERT_ID()", ex);
		}
	}

	@Override
	public void restart() {
		try (Connection con = getDataSource().getConnection()) {
			con.setAutoCommit(true);
			try (Statement stmt = con.createStatement()) {
				String sequenceName = getSequenceName();
				stmt.executeUpdate("UPDATE `" + getTableName() + "` SET VALUE = 0 WHERE NAME='" + sequenceName + "'");
				nextId = 0;
				maxId = 0;
			}
		} catch (SQLException ex) {
			throw new DataAccessResourceFailureException(ex.getMessage(), ex);
		}
	}

	public static void createTable(DataSource dataSrouce, String tableName, String sequenceName) throws SQLException {
		try (Connection conn = dataSrouce.getConnection(); Statement stmt = conn.createStatement()) {
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
						+ "` (NAME VARCHAR(50) NOT NULL PRIMARY KEY, VALUE INT NOT NULL DEFAULT 0, LAST_UPDATED BIGINT NOT NULL) ");
				stmt.execute("INSERT INTO `" + tableName + "` VALUES('" + sequenceName + "',0,UNIX_TIMESTAMP())");
			}
		}
	}
}
