package org.ironrhino.sample.polling;

import org.ironrhino.core.model.Displayable;

public enum BalanceQueryStatus implements Displayable {

	INITIALIZED, PROCESSING, SUCCESSFUL, FAILED;

	@Override
	public String toString() {
		return getDisplayName();
	}
}
