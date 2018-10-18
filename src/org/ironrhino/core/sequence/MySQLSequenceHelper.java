package org.ironrhino.core.sequence;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import javax.sql.DataSource;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MySQLSequenceHelper {

	public static void createOrUpgradeTable(DataSource dataSrouce, String tableName, String sequenceName)
			throws SQLException {
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
					Arrays.asList(tableName.toUpperCase(), tableName, tableName.toLowerCase()))) {
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
					try (PreparedStatement ps = conn.prepareStatement("INSERT INTO " + tableName + " VALUES(?,?,?)")) {
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
