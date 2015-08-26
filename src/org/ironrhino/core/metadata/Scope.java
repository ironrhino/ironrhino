package org.ironrhino.core.metadata;

import org.ironrhino.core.model.Displayable;

public enum Scope implements Displayable {
	LOCAL, // only this jvm
	APPLICATION, // all jvm for this application
	GLOBAL; // all jvm for all application

	public String getName() {
		return name();
	}

	public String getDisplayName() {
		return Displayable.super.getDisplayName();
	}

	public static Scope parse(String name) {
		if (name != null)
			for (Scope en : values())
				if (name.equals(en.name()) || name.equals(en.getDisplayName()))
					return en;
		return null;
	}

	@Override
	public String toString() {
		return getDisplayName();
	}
}
