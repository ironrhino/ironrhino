package org.ironrhino.core.model;

import java.util.Collection;

public interface Treeable<T extends Treeable<T>> {

	default int getLevel() {
		int level = 1;
		T parent = getParent();
		while (parent != null) {
			level++;
			parent = parent.getParent();
		}
		return level;
	}

	T getParent();

	void setParent(T parent);

	Collection<T> getChildren();

	void setChildren(Collection<T> children);

	@SuppressWarnings("unchecked")
	default void addChild(T... children) {
		for (T child : children) {
			child.setParent((T) this);
			getChildren().add(child);
		}
	}

}
