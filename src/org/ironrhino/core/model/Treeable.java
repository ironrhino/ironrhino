package org.ironrhino.core.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;

@SuppressWarnings("unchecked")
public interface Treeable<T extends Treeable<T, ID>, ID> {

	default ID getId() {
		return null;
	}

	String getName();

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

	default void addChild(T... children) {
		for (T child : children) {
			child.setParent((T) this);
			getChildren().add(child);
		}
	}

	@JsonIgnore
	default boolean isLeaf() {
		return CollectionUtils.isEmpty(getChildren());
	}

	@JsonIgnore
	default boolean isRoot() {
		return this.getParent() == null;
	}

	default boolean isHasChildren() {
		return !isLeaf();
	}

	default T getDescendantOrSelfById(ID id) {
		if (id == null)
			throw new IllegalArgumentException("id must not be null");
		if (id.equals(this.getId()))
			return (T) this;
		for (T t : getChildren()) {
			T tt = t.getDescendantOrSelfById(id);
			if (tt != null)
				return tt;
		}
		return null;
	}

	default T getDescendantOrSelfByName(String name) {
		if (name == null)
			throw new IllegalArgumentException("name must not be null");
		if (name.equals(this.getName()))
			return (T) this;
		for (T t : getChildren()) {
			T tt = t.getDescendantOrSelfByName(name);
			if (tt != null)
				return tt;
		}
		return null;
	}

	@JsonIgnore
	default List<T> getDescendants() {
		List<T> list = new ArrayList<>();
		if (!this.isLeaf())
			for (T obj : this.getChildren()) {
				collect(obj, list);
			}
		return list;
	}

	@JsonIgnore
	default List<T> getDescendantsAndSelf() {
		List<T> list = new ArrayList<>();
		collect((T) this, list);
		return list;
	}

	default void collect(T node, Collection<T> coll) {
		coll.add(node);
		if (node.isLeaf())
			return;
		for (T obj : node.getChildren()) {
			collect(obj, coll);
		}
	}

	default boolean isAncestorOrSelfOf(T t) {
		T parent = t;
		while (parent != null) {
			if (parent.getId().equals(this.getId()))
				return true;
			parent = parent.getParent();
		}
		return false;
	}

	default boolean isDescendantOrSelfOf(T t) {
		return t != null && t.isAncestorOrSelfOf((T) this);
	}

	default T getAncestor(int level) {
		if (level < 1 || level > this.getLevel())
			return null;
		T parent = (T) this;
		while (parent != null) {
			if (parent.getLevel() == level)
				return parent;
			parent = parent.getParent();
		}
		return null;
	}

}
