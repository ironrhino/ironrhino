package org.ironrhino.common.model;

import org.ironrhino.core.model.Displayable;

public enum PollingStatus implements Displayable {

	INITIALIZED, PROCESSING, TEMPORARY_ERROR, SUCCESSFUL, FAILED;

	@Override
	public String toString() {
		return getDisplayName();
	}
}
