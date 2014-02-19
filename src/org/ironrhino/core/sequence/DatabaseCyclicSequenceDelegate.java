package org.ironrhino.core.sequence;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

import org.ironrhino.core.jdbc.DatabaseProduct;
import org.springframework.jdbc.datasource.DataSourceUtils;

public class DatabaseCyclicSequenceDelegate extends
		AbstractDatabaseCyclicSequence {

	private AbstractDatabaseCyclicSequence seq = null;

	@Override
	public void afterPropertiesSet() throws java.lang.Exception {
		Connection con = DataSourceUtils.getConnection(getDataSource());
		DatabaseProduct databaseProduct = null;
		try {
			DatabaseMetaData dbmd = con.getMetaData();
			databaseProduct = DatabaseProduct.parse(dbmd
					.getDatabaseProductName().toLowerCase());
		} finally {
			DataSourceUtils.releaseConnection(con, getDataSource());
		}
		if (databaseProduct == DatabaseProduct.MYSQL)
			seq = new MySQLCyclicSequence();
		else if (databaseProduct == DatabaseProduct.POSTGRESQL)
			seq = new PostgreSQLCyclicSequence();
		else if (databaseProduct == DatabaseProduct.ORACLE)
			seq = new OracleCyclicSequence();
		else if (databaseProduct == DatabaseProduct.DB2)
			seq = new DB2CyclicSequence();
		else if (databaseProduct == DatabaseProduct.INFORMIX)
			seq = new InformixCyclicSequence();
		else if (databaseProduct == DatabaseProduct.SQLSERVER)
			seq = new SqlServerCyclicSequence();
		else if (databaseProduct == DatabaseProduct.SYBASE)
			seq = new SybaseCyclicSequence();
		else if (databaseProduct == DatabaseProduct.H2)
			seq = new H2CyclicSequence();
		else if (databaseProduct == DatabaseProduct.HSQL)
			seq = new HSQLCyclicSequence();
		else if (databaseProduct == DatabaseProduct.DERBY)
			seq = new DerbyCyclicSequence();
		else
			throw new RuntimeException("not implemented for database "
					+ databaseProduct);
		seq.setDataSource(getDataSource());
		if (getCacheSize() > 1)
			seq.setCacheSize(getCacheSize());
		seq.setCycleType(getCycleType());
		seq.setPaddingLength(getPaddingLength());
		seq.setTableName(getTableName());
		seq.setSequenceName(getSequenceName());
		seq.setLockService(getLockService());
		seq.afterPropertiesSet();
	}

	@Override
	public String nextStringValue() {
		return seq.nextStringValue();
	}

}
