package org.ironrhino.core.hibernate.dialect;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MySQL57InnoDBDialect;
import org.hibernate.engine.jdbc.dialect.internal.StandardDialectResolver;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.ironrhino.core.hibernate.CriterionUtils;
import org.ironrhino.core.jdbc.DatabaseProduct;

public class MyDialectResolver extends StandardDialectResolver {

	private static final long serialVersionUID = -3451798629900051614L;

	@Override
	public Dialect resolveDialect(DialectResolutionInfo info) {
		String databaseName = info.getDatabaseName();
		DatabaseProduct database = DatabaseProduct.parse(databaseName);
		CriterionUtils.DATABASE_PRODUCT = database;
		if (database == DatabaseProduct.MYSQL) {
			int majorVersion = info.getDatabaseMajorVersion();
			int minorVersion = info.getDatabaseMinorVersion();
			if (majorVersion > 5 || majorVersion == 5 && minorVersion >= 7)
				return new MySQL57InnoDBDialect();
			else if (majorVersion == 5 && minorVersion >= 6)
				return new MySQL56Dialect();
		}
		return super.resolveDialect(info);
	}
}
