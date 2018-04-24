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

	public void setParent(T parent);

	public Collection<T> getChildren();

	public void setChildren(Collection<T> children);

	@SuppressWarnings("unchecked")
	public default void addChild(T... children) {
		for (T child : children) {
			child.setParent((T) this);
			getChildren().add(child);
		}
	}

}
