package org.ironrhino.core.jdbc;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

public class DatabaseProductTest {

	@Test
	public void testParse() {
		assertThat(DatabaseProduct.parse("MySQL"), is(DatabaseProduct.MYSQL));
		assertThat(DatabaseProduct.parse("jdbc:mysql://localhost:3306/test"), is(DatabaseProduct.MYSQL));
		assertThat(DatabaseProduct.parse("MariaDB"), is(DatabaseProduct.MARIADB));
		assertThat(DatabaseProduct.parse("jdbc:mariadb://localhost:3306/test"), is(DatabaseProduct.MARIADB));
		assertThat(DatabaseProduct.parse("PostgreSQL"), is(DatabaseProduct.POSTGRESQL));
		assertThat(DatabaseProduct.parse("jdbc:postgresql://localhost:5432/test"), is(DatabaseProduct.POSTGRESQL));
		assertThat(DatabaseProduct.parse("Oracle"), is(DatabaseProduct.ORACLE));
		assertThat(DatabaseProduct.parse("jdbc:oracle:thin:@//localhost:1521/XE"), is(DatabaseProduct.ORACLE));
		assertThat(DatabaseProduct.parse("DB2"), is(DatabaseProduct.DB2));
		assertThat(DatabaseProduct.parse("jdbc:db2://localhost:50000/test"), is(DatabaseProduct.DB2));
		assertThat(DatabaseProduct.parse("Microsoft SQL Server"), is(DatabaseProduct.SQLSERVER));
		assertThat(DatabaseProduct.parse("jdbc:sqlserver://localhost:1433;Database=test"),
				is(DatabaseProduct.SQLSERVER));
	}

}
