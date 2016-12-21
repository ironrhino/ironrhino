package org.ironrhino.sample.crud;

import org.ironrhino.core.model.Displayable;

public enum CustomerRank implements Displayable {

	BRONZE, SILVER, GOLD;

	@Override
	public String getName() {
		return Displayable.super.getName();
	}

	@Override
	public String getDisplayName() {
		return Displayable.super.getDisplayName();
	}

	@Override
	public String toString() {
		return getDisplayName();
	}
}
