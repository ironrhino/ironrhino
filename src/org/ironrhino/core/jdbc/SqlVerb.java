package org.ironrhino.core.jdbc;

import java.util.Locale;

import org.ironrhino.core.model.Displayable;

public enum SqlVerb implements Displayable {

	SELECT, UPDATE, INSERT, MERGE, DELETE, CREATE, ALTER, DROP, CALL, RENAME, COMMENT, GRANT, REVOKE, BACKUP;

	@Override
	public String getName() {
		return name();
	}

	@Override
	public String getDisplayName() {
		return Displayable.super.getDisplayName();
	}

	public static SqlVerb parse(String name) {
		if (name != null)
			for (SqlVerb en : values())
				if (name.equals(en.name()) || name.equals(en.getDisplayName()))
					return en;
		return null;
	}

	public static SqlVerb parseBySql(String sql) {
		sql = SqlUtils.clearComments(sql).trim();
		String[] arr = sql.split("\\s");
		try {
			return valueOf(arr[0].toUpperCase(Locale.ROOT));
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public String toString() {
		return getDisplayName();
	}

}
