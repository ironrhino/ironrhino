package org.ironrhino.common.model;

import org.ironrhino.core.model.Displayable;
import org.ironrhino.core.struts.I18N;

public enum SchemaFieldType implements Displayable {
	SELECT, CHECKBOX, INPUT, GROUP;
	@Override
	public String getName() {
		return name();
	}

	@Override
	public String getDisplayName() {
		try {
			return I18N.getText(getClass(), name());
		} catch (Exception e) {
			return name();
		}
	}

	public static SchemaFieldType parse(String name) {
		if (name != null)
			for (SchemaFieldType en : values())
				if (name.equals(en.name()) || name.equals(en.getDisplayName()))
					return en;
		return null;
	}

	@Override
	public String toString() {
		return getDisplayName();
	}
}
