package org.ironrhino.common.model;

import org.ironrhino.core.model.Displayable;

public enum SchemaFieldType implements Displayable {
	SELECT, CHECKBOX, INPUT, GROUP;

	@Override
	public String toString() {
		return getDisplayName();
	}
}
