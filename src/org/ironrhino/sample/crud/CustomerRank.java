package org.ironrhino.sample.crud;

import org.ironrhino.core.model.Displayable;

public enum CustomerRank implements Displayable {

	BRONZE, SILVER, GOLD;

	@Override
	public String toString() {
		return getDisplayName();
	}
}
