package org.ironrhino.core.sequence.simple;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

import org.ironrhino.core.jdbc.DatabaseProduct;
import org.springframework.jdbc.datasource.DataSourceUtils;

public class DatabaseSimpleSequenceDelegate extends AbstractDatabaseSimpleSequence {

	private AbstractDatabaseSimpleSequence seq = null;

	@Override
	public void afterPropertiesSet() throws java.lang.Exception {
		Connection con = DataSourceUtils.getConnection(getDataSource());
		DatabaseProduct databaseProduct = null;
		try {
			DatabaseMetaData dbmd = con.getMetaData();
			databaseProduct = DatabaseProduct.parse(dbmd.getDatabaseProductName().toLowerCase());
		} finally {
			DataSourceUtils.releaseConnection(con, getDataSource());
		}
		if (databaseProduct == DatabaseProduct.MYSQL)
			seq = new MySQLSimpleSequence();
		else if (databaseProduct == DatabaseProduct.POSTGRESQL)
			seq = new PostgreSQLSimpleSequence();
		else if (databaseProduct == DatabaseProduct.ORACLE)
			seq = new OracleSimpleSequence();
		else if (databaseProduct == DatabaseProduct.DB2)
			seq = new DB2SimpleSequence();
		else if (databaseProduct == DatabaseProduct.INFORMIX)
			seq = new InformixSimpleSequence();
		else if (databaseProduct == DatabaseProduct.SQLSERVER)
			seq = new SqlServerSimpleSequence();
		else if (databaseProduct == DatabaseProduct.SYBASE)
			seq = new SybaseSimpleSequence();
		else if (databaseProduct == DatabaseProduct.H2)
			seq = new H2SimpleSequence();
		else if (databaseProduct == DatabaseProduct.HSQL)
			seq = new HSQLSimpleSequence();
		else if (databaseProduct == DatabaseProduct.DERBY)
			seq = new DerbySimpleSequence();
		else
			throw new RuntimeException("not implemented for database " + databaseProduct);
		seq.setDataSource(getDataSource());
		if (getCacheSize() > 1)
			seq.setCacheSize(getCacheSize());
		seq.setPaddingLength(getPaddingLength());
		seq.setTableName(getTableName());
		seq.setSequenceName(getSequenceName());
		seq.afterPropertiesSet();
	}

	@Override
	public void restart() {
		seq.restart();
	}

	@Override
	public int nextIntValue() {
		return seq.nextIntValue();
	}

}
