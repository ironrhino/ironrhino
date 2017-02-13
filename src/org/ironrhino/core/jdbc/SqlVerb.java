package org.ironrhino.core.jdbc;

import java.util.Locale;

import org.ironrhino.core.model.Displayable;

public enum SqlVerb implements Displayable {

	SELECT, UPDATE, INSERT, MERGE, DELETE, CREATE, ALTER, DROP, CALL, RENAME, COMMENT, GRANT, REVOKE, BACKUP;

	public static SqlVerb parseBySql(String sql) {
		sql = SqlUtils.clearComments(sql).trim();
		String[] arr = sql.split("\\s");
		try {
			if (arr[0].toUpperCase(Locale.ROOT).equals("WITH"))
				return SELECT;
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
