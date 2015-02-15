package org.ironrhino.core.jdbc;

import org.ironrhino.core.model.Displayable;
import org.ironrhino.core.struts.I18N;

public enum SqlVerb implements Displayable {

	SELECT, UPDATE, INSERT, MERGE, DELETE, CREATE, ALTER, DROP, CALL, RENAME, COMMENT, GRANT, REVOKE, BACKUP;

	@Override
	public String getName() {
		return this.name();
	}

	@Override
	public String getDisplayName() {
		try {
			return I18N.getText(getClass(), name());
		} catch (Exception e) {
			return name();
		}
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
			return valueOf(arr[0].toUpperCase());
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public String toString() {
		return getDisplayName();
	}

}
