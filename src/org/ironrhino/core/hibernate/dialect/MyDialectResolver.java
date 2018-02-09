package org.ironrhino.core.hibernate.dialect;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.internal.StandardDialectResolver;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolver;
import org.ironrhino.core.jdbc.DatabaseProduct;

public class MyDialectResolver implements DialectResolver {

	private static final long serialVersionUID = -3451798629900051614L;

	private DialectResolver standardDialectResolver = new StandardDialectResolver();

	@Override
	public Dialect resolveDialect(DialectResolutionInfo info) {
		DatabaseProduct database = DatabaseProduct.parse(info.getDatabaseName());
		int majorVersion = info.getDatabaseMajorVersion();
		int minorVersion = info.getDatabaseMinorVersion();
		if (database == DatabaseProduct.MYSQL) {
			if (majorVersion == 5 && minorVersion == 6)
				return new MySQL56Dialect();
		}
		return standardDialectResolver.resolveDialect(info);
	}
}
