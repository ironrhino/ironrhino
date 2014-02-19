package org.ironrhino.core.hibernate.dialect;

import java.sql.Types;

import org.hibernate.dialect.MySQL5Dialect;

public class MySQL56Dialect extends MySQL5Dialect {
	public MySQL56Dialect() {
		super();
		registerColumnType(Types.TIMESTAMP, "datetime(6)");
	}
}
