package org.ironrhino.core.sequence.simple;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataAccessResourceFailureException;

public class MySQLSimpleSequence extends AbstractDatabaseSimpleSequence {

	@Override
	public void afterPropertiesSet() {
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
					stmt.execute("INSERT INTO `" + tableName + "` VALUES('" + sequenceName + "',0,0)");
				}
			} else {
				stmt.execute("CREATE TABLE `" + tableName
						+ "` (NAME VARCHAR(50) PRIMARY KEY, VALUE INT NOT NULL DEFAULT 0, LAST_UPDATED BIGINT) ");
				stmt.execute("INSERT INTO `" + tableName + "` VALUES('" + sequenceName + "',0,0)");
			}
		} catch (SQLException ex) {
			logger.error(ex.getMessage(), ex);
		}
	}

	@Override
	public int nextIntValue() {
		try (Connection con = getDataSource().getConnection()) {
			con.setAutoCommit(true);
			try (Statement stmt = con.createStatement()) {
				String sequenceName = getSequenceName();
				stmt.executeUpdate("UPDATE `" + getTableName() + "` SET VALUE = LAST_INSERT_ID(VALUE + 1) WHERE NAME='"
						+ sequenceName + "'");
				try (ResultSet rs = stmt.executeQuery("SELECT LAST_INSERT_ID()")) {
					if (!rs.next()) {
						throw new DataAccessResourceFailureException(
								"LAST_INSERT_ID() failed after executing an update");
					}
					return rs.getInt(1);
				}
			}
		} catch (SQLException ex) {
			throw new DataAccessResourceFailureException("Could not obtain last_insert_id()", ex);
		}
	}

	@Override
	public void restart() {
		try (Connection con = getDataSource().getConnection()) {
			con.setAutoCommit(true);
			try (Statement stmt = con.createStatement()) {
				String sequenceName = getSequenceName();
				stmt.executeUpdate("UPDATE `" + getTableName() + "` SET VALUE = 0 WHERE NAME='" + sequenceName + "'");
			}
		} catch (SQLException ex) {
			throw new DataAccessResourceFailureException(ex.getMessage(), ex);
		}
	}
}
