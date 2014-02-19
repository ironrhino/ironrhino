package org.ironrhino.core.hibernate.dialect;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.internal.StandardDialectResolver;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;

public class MyDialectResolver extends StandardDialectResolver {

	private static final long serialVersionUID = -3451798629900051614L;

	@Override
	public Dialect resolveDialect(DialectResolutionInfo info) {
		final String databaseName = info.getDatabaseName();
		if ("MySQL".equals(databaseName)) {
			final int majorVersion = info.getDatabaseMajorVersion();
			final int minorVersion = info.getDatabaseMinorVersion();
			if (majorVersion > 5 || majorVersion == 5 && minorVersion >= 6)
				return new MySQL56Dialect();
		}
		return super.resolveDialect(info);
	}
}
