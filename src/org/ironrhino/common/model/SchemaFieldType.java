package org.ironrhino.common.model;

import org.ironrhino.core.model.Displayable;

public enum SchemaFieldType implements Displayable {
	SELECT, CHECKBOX, INPUT, GROUP;

	public String getName() {
		return name();
	}

	public String getDisplayName() {
		return Displayable.super.getDisplayName();
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
