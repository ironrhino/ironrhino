package org.ironrhino.core.sequence.simple;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.ironrhino.core.sequence.MySQLSequenceHelper;
import org.springframework.dao.DataAccessResourceFailureException;

public class MySQLSimpleSequence extends AbstractDatabaseSimpleSequence {

	private long nextId = 0;

	private long maxId = 0;

	@Override
	public void afterPropertiesSet() {
		try {
			MySQLSequenceHelper.createOrUpgradeTable(getDataSource(), getTableName(), getSequenceName());
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
}
