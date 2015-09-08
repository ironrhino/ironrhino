package org.ironrhino.core.hibernate.dialect;

import java.sql.Types;

import org.hibernate.dialect.MySQL5Dialect;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.dialect.function.StaticPrecisionFspTimestampFunction;

public class MySQL56Dialect extends MySQL5Dialect {
	public MySQL56Dialect() {
		super();
		registerColumnType(Types.TIMESTAMP, "datetime(6)");

		// cp from MySQL57InnoDBDialect
		final SQLFunction currentTimestampFunction = new StaticPrecisionFspTimestampFunction("now", 6);
		registerFunction("now", currentTimestampFunction);
		registerFunction("current_timestamp", currentTimestampFunction);
		registerFunction("localtime", currentTimestampFunction);
		registerFunction("localtimestamp", currentTimestampFunction);
		registerFunction("sysdate", new StaticPrecisionFspTimestampFunction("sysdate", 6));
	}
}
