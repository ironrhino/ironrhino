package org.ironrhino.common.model;

import org.ironrhino.core.model.Displayable;

public enum Gender implements Displayable {

	MALE, FEMALE;

	@Override
	public String getName() {
		return Displayable.super.getName();
	}

	@Override
	public String getDisplayName() {
		return Displayable.super.getDisplayName();
	}

	public static Gender parse(String name) {
		if (name != null)
			for (Gender en : values())
				if (name.equals(en.name()) || name.equals(en.getDisplayName()))
					return en;
		return null;
	}

	@Override
	public String toString() {
		return getDisplayName();
	}
}
