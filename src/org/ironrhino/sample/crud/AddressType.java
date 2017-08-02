package org.ironrhino.sample.crud;

import org.ironrhino.core.model.Displayable;

public enum AddressType implements Displayable {

	HOME, WORK, OTHER;

	@Override
	public String toString() {
		return getDisplayName();
	}
}
