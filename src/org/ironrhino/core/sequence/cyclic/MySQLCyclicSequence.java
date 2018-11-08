package org.ironrhino.core.sequence.cyclic;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import org.ironrhino.core.sequence.MySQLSequenceHelper;
import org.ironrhino.core.util.MaxAttemptsExceededException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;

public class MySQLCyclicSequence extends AbstractDatabaseCyclicSequence {

	private String setVariableSql;
	private String incrementSql;
	private String restartSql;
	private String selectLastInsertIdSql = "SELECT LAST_INSERT_ID(),@TIMESTAMP";

	@Override
	public void afterPropertiesSet() {
		setVariableSql = new StringBuilder("SELECT @TIMESTAMP:=GREATEST(LAST_UPDATED,UNIX_TIMESTAMP()) FROM `")
				.append(getTableName()).append("` WHERE NAME='").append(getSequenceName()).append("'").toString();
		incrementSql = new StringBuilder("UPDATE `").append(getTableName())
				.append("` SET VALUE=LAST_INSERT_ID(VALUE+1),LAST_UPDATED=@TIMESTAMP WHERE NAME='")
				.append(getSequenceName()).append("' AND DATE_FORMAT(FROM_UNIXTIME(LAST_UPDATED),'")
				.append(getDateFormat()).append("')=DATE_FORMAT(FROM_UNIXTIME(@TIMESTAMP),'").append(getDateFormat())
				.append("')").toString();
		restartSql = new StringBuilder("UPDATE `").append(getTableName())
				.append("` SET VALUE=LAST_INSERT_ID(1),LAST_UPDATED=@TIMESTAMP WHERE NAME='").append(getSequenceName())
				.append("' AND DATE_FORMAT(FROM_UNIXTIME(LAST_UPDATED),'").append(getDateFormat())
				.append("')!=DATE_FORMAT(FROM_UNIXTIME(@TIMESTAMP),'").append(getDateFormat()).append("')").toString();
		try {
			MySQLSequenceHelper.createOrUpgradeTable(getDataSource(), getTableName(), getSequenceName());
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public String nextStringValue() throws DataAccessException {
		try (Connection con = getDataSource().getConnection(); Statement stmt = con.createStatement()) {
			con.setAutoCommit(true);
			int maxAttempts = 3;
			int attempts = maxAttempts;
			do {
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
			} while (--attempts > 0);
			throw new MaxAttemptsExceededException(maxAttempts);
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
