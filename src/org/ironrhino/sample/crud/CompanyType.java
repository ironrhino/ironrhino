package org.ironrhino.sample.crud;

import org.ironrhino.core.model.Displayable;

public enum CompanyType implements Displayable {

	STATE_OWNED, PRIVATE, PUBLIC;

	@Override
	public String toString() {
		return getDisplayName();
	}
}
