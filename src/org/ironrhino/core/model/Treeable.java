package org.ironrhino.core.model;

import java.util.Collection;

public interface Treeable<T extends Treeable<T>> {

	public default int getLevel() {
		int level = 1;
		T parent = getParent();
		while (parent != null) {
			level++;
			parent = parent.getParent();
		}
		return level;
	}

	public T getParent();

	public Collection<T> getChildren();
}
