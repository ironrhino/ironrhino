package org.ironrhino.core.sequence.simple;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.ironrhino.core.sequence.MySQLSequenceHelper;
import org.springframework.dao.DataAccessResourceFailureException;

public class MySQLSimpleSequence extends AbstractDatabaseSimpleSequence {

	@Override
	public void afterPropertiesSet() {
		try {
			MySQLSequenceHelper.createOrUpgradeTable(getDataSource(), getTableName(), getSequenceName());
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
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
