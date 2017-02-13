package org.ironrhino.common.model;

import org.ironrhino.core.model.Displayable;

public enum Gender implements Displayable {

	MALE, FEMALE;

	@Override
	public String toString() {
		return getDisplayName();
	}
}
