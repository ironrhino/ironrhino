package org.ironrhino.core.model;

import org.ironrhino.core.struts.I18N;

public interface Displayable {

	String getName();

	default String getDisplayName() {
		try {
			return I18N.getText(getClass(), getName());
		} catch (Exception e) {
			return getName();
		}
	}

}
