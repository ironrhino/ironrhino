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

public class MySQLCyclicSequence extends AbstractDatabaseCyclicSequence {

	private String incrementSql;
	private String restartSql;
	private String setVariableSql = "SELECT @TIMESTAMP:=UNIX_TIMESTAMP()";
	private String selectLastInsertIdSql = "SELECT LAST_INSERT_ID(),@TIMESTAMP";

	@Override
	public void afterPropertiesSet() {
		incrementSql = new StringBuilder("UPDATE `").append(getTableName())
				.append("` SET VALUE=LAST_INSERT_ID(VALUE+1),LAST_UPDATED=@TIMESTAMP WHERE NAME='")
				.append(getSequenceName()).append("' AND DATE_FORMAT(FROM_UNIXTIME(LAST_UPDATED),'")
				.append(getDateFormat()).append("')=DATE_FORMAT(FROM_UNIXTIME(@TIMESTAMP),'").append(getDateFormat())
				.append("')").toString();
		restartSql = new StringBuilder("UPDATE `").append(getTableName())
				.append("` SET VALUE=LAST_INSERT_ID(1),LAST_UPDATED=@TIMESTAMP WHERE NAME='").append(getSequenceName())
				.append("' AND DATE_FORMAT(FROM_UNIXTIME(LAST_UPDATED),'").append(getDateFormat())
				.append("')!=DATE_FORMAT(FROM_UNIXTIME(@TIMESTAMP),'").append(getDateFormat()).append("')").toString();

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
		try (Connection con = getDataSource().getConnection(); Statement stmt = con.createStatement()) {
			con.setAutoCommit(true);
			int maxAttempts = 3;
			while (--maxAttempts > 0) {
				stmt.execute(setVariableSql);
				int rows = stmt.executeUpdate(incrementSql);
				if (rows == 1)
					return nextId(stmt);
				stmt.execute(setVariableSql);
				rows = stmt.executeUpdate(restartSql);
				if (rows == 1)
					return nextId(stmt);
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			throw new IllegalStateException("max attempts reached");
		} catch (SQLException ex) {
			throw new DataAccessResourceFailureException("Could not obtain last_insert_id()", ex);
		}

	}

	private String nextId(Statement stmt) throws SQLException {
		try (ResultSet rs = stmt.executeQuery(selectLastInsertIdSql)) {
			if (!rs.next())
				throw new DataAccessResourceFailureException("LAST_INSERT_ID() failed after executing an update");
			int next = rs.getInt(1);
			Long current = rs.getLong(2);
			if (current < 10000000000L) // no mills
				current *= 1000;
			Date currentTimestamp = new Date(current);
			return getStringValue(currentTimestamp, getPaddingLength(), next);
		}
	}

	private String getDateFormat() {
		switch (getCycleType()) {
		case MINUTE:
			return "%Y%m%d%H%i";
		case HOUR:
			return "%Y%m%d%H";
		case DAY:
			return "%Y%m%d";
		case MONTH:
			return "%Y%m";
		case YEAR:
			return "%Y";
		default:
			throw new UnsupportedOperationException("Unknown cycle type");
		}
	}

}
