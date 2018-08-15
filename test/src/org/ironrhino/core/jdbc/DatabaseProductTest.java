package org.ironrhino.core.jdbc;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DatabaseProductTest {

	@Test
	public void testParse() {
		assertEquals(DatabaseProduct.MYSQL, DatabaseProduct.parse("MySQL"));
		assertEquals(DatabaseProduct.MYSQL, DatabaseProduct.parse("jdbc:mysql://localhost:3306/test"));
		assertEquals(DatabaseProduct.POSTGRESQL, DatabaseProduct.parse("PostgreSQL"));
		assertEquals(DatabaseProduct.POSTGRESQL, DatabaseProduct.parse("jdbc:postgresql://localhost:5432/test"));
		assertEquals(DatabaseProduct.ORACLE, DatabaseProduct.parse("Oracle"));
		assertEquals(DatabaseProduct.ORACLE, DatabaseProduct.parse("jdbc:oracle:thin:@//localhost:1521/XE"));
		assertEquals(DatabaseProduct.DB2, DatabaseProduct.parse("DB2"));
		assertEquals(DatabaseProduct.DB2, DatabaseProduct.parse("jdbc:db2://localhost:50000/test"));
		assertEquals(DatabaseProduct.SQLSERVER, DatabaseProduct.parse("Microsoft SQL Server"));
		assertEquals(DatabaseProduct.SQLSERVER, DatabaseProduct.parse("jdbc:sqlserver://localhost:1433;Database=test"));
	}

}
