package org.ironrhino.core.sequence.simple;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Locale;

import javax.sql.DataSource;

import org.ironrhino.core.jdbc.DatabaseProduct;

public class DatabaseSimpleSequenceDelegate extends AbstractDatabaseSimpleSequence {

	private AbstractDatabaseSimpleSequence seq = null;

	public DatabaseSimpleSequenceDelegate() {

	}

	public DatabaseSimpleSequenceDelegate(DataSource dataSource) {
		setDataSource(dataSource);
	}

	@Override
	public void afterPropertiesSet() throws java.lang.Exception {
		DatabaseProduct databaseProduct = null;
		try (Connection con = getDataSource().getConnection()) {
			DatabaseMetaData dbmd = con.getMetaData();
			databaseProduct = DatabaseProduct.parse(dbmd.getDatabaseProductName().toLowerCase(Locale.ROOT));
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
