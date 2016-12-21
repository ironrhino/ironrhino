package org.ironrhino.core.hibernate.dialect;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MySQL57Dialect;
import org.hibernate.dialect.PostgreSQL95Dialect;
import org.hibernate.engine.jdbc.dialect.internal.StandardDialectResolver;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.ironrhino.core.jdbc.DatabaseProduct;

public class MyDialectResolver extends StandardDialectResolver {

	private static final long serialVersionUID = -3451798629900051614L;

	@Override
	public Dialect resolveDialect(DialectResolutionInfo info) {
		String databaseName = info.getDatabaseName();
		int majorVersion = info.getDatabaseMajorVersion();
		int minorVersion = info.getDatabaseMinorVersion();
		DatabaseProduct database = DatabaseProduct.parse(databaseName);
		if (database == DatabaseProduct.MYSQL) {
			if (majorVersion > 5 || majorVersion == 5 && minorVersion >= 7)
				return new MySQL57Dialect();
			else if (majorVersion == 5 && minorVersion >= 6)
				return new MySQL56Dialect();
		} else if (database == DatabaseProduct.POSTGRESQL) {
			if (majorVersion >= 10)
				return new PostgreSQL95Dialect();
		}
		return super.resolveDialect(info);
	}
}
