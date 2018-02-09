package org.ironrhino.core.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.metadata.CaseInsensitive;
import org.ironrhino.core.metadata.FullnameSeperator;
import org.ironrhino.core.metadata.NotInCopy;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.search.elasticsearch.annotations.SearchableId;
import org.ironrhino.core.search.elasticsearch.annotations.SearchableProperty;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@SuppressWarnings("unchecked")
@MappedSuperclass
@Getter
@Setter
public abstract class BaseTreeableEntity<T extends BaseTreeableEntity<T>> extends AbstractEntity<Long>
		implements Treeable<T>, Ordered<T> {

	private static final long serialVersionUID = 2462271646391940930L;

	@Id
	@SearchableId
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "treeable_entity_seq")
	protected Long id;

	@Column(unique = true)
	@UiConfig(hidden = true)
	protected String fullId;

	@Column(nullable = false)
	@UiConfig(cssClass = "checkavailable")
	@SearchableProperty
	protected String name;

	@UiConfig(hidden = true)
	protected int level;

	@SearchableProperty
	@UiConfig(displayOrder = 1000, width = "100px")
	protected int displayOrder;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parentId")
	protected T parent;

	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, mappedBy = "parent")
	@OrderBy("displayOrder,name")
	protected Collection<T> children = new ArrayList<>(0);

	@JsonIgnore
	public String getFullId() {
		return fullId;
	}

	@CaseInsensitive
	@SearchableProperty(boost = 3)
	public String getName() {
		return name;
	}

	@JsonIgnore
	public String getFullnameSeperator() {
		FullnameSeperator fs = getClass().getAnnotation(FullnameSeperator.class);
		if (fs != null)
			return fs.seperator();
		return "/";
	}

	public String getFullname() {
		return getFullname(getFullnameSeperator(), null);
	}

	public String getFullname(Long tree) {
		return getFullname(getFullnameSeperator(), tree);
	}

	public String getFullname(String seperator) {
		return getFullname(seperator, null);
	}

	public String getFullname(String seperator, Long tree) {
		if (name == null)
			return null;
		StringBuilder fullname = new StringBuilder(name);
		T e = (T) this;
		while ((e = e.getParent()) != null) {
			if (!(e.isRoot() && StringUtils.isBlank(e.getName())))
				fullname.insert(0, e.getName() + seperator);
			if (tree != null && tree.equals(e.getId()))
				break;
		}
		return fullname.toString();
	}

	@Override
	@JsonIgnore
	public int getDisplayOrder() {
		return displayOrder;
	}

	@Override
	@NotInCopy
	@JsonIgnore
	@UiConfig(hidden = true)
	public Collection<T> getChildren() {
		return children;
	}

	@JsonIgnore
	public boolean isLeaf() {
		return this.children == null || this.children.size() == 0;
	}

	public boolean isHasChildren() {
		return !isLeaf();
	}

	@JsonIgnore
	public boolean isRoot() {
		return this.parent == null;
	}

	@Override
	@NotInCopy
	@JsonIgnore
	@UiConfig(hidden = true)
	public T getParent() {
		return parent;
	}

	public T getDescendantOrSelfById(Long id) {
		if (id == null)
			throw new IllegalArgumentException("id must not be null");
		if (id.equals(this.getId()))
			return (T) this;
		for (T t : getChildren()) {
			if (id.equals(t.getId())) {
				return t;
			} else {
				T tt = t.getDescendantOrSelfById(id);
				if (tt != null)
					return tt;
			}
		}
		return null;
	}

	public T getDescendantOrSelfByName(String name) {
		if (name == null)
			throw new IllegalArgumentException("name must not be null");
		if (name.equals(this.getName()))
			return (T) this;
		for (T t : getChildren()) {
			if (name.equals(t.getName())) {
				return t;
			} else {
				T tt = t.getDescendantOrSelfByName(name);
				if (tt != null)
					return tt;
			}
		}
		return null;
	}

	@JsonIgnore
	public List<T> getDescendants() {
		List<T> ids = new ArrayList<>();
		if (!this.isLeaf())
			for (T obj : this.getChildren()) {
				collect(obj, ids);
			}
		return ids;
	}

	@JsonIgnore
	public List<T> getDescendantsAndSelf() {
		List<T> ids = new ArrayList<>();
		collect((T) this, ids);
		return ids;
	}

	private void collect(T node, Collection<T> coll) {
		coll.add(node);
		if (node.isLeaf())
			return;
		for (T obj : node.getChildren()) {
			collect(obj, coll);
		}
	}

	public boolean isAncestorOrSelfOf(T t) {
		T parent = t;
		while (parent != null) {
			if (parent.getId().equals(this.getId()))
				return true;
			parent = parent.getParent();
		}
		return false;
	}

	public boolean isDescendantOrSelfOf(T t) {
		return t != null && t.isAncestorOrSelfOf((T) this);
	}

	public T getAncestor(int level) {
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

	@Override
	public int hashCode() {
		if (id != null)
			return id.intValue();
		String fullname = getFullname(",");
		return fullname != null ? fullname.hashCode() : 0;
	}

	@Override
	public boolean equals(Object object) {
		if (object == this)
			return true;
		if (object == null || this.getClass() != object.getClass() || this.hashCode() == 0 || object.hashCode() == 0)
			return false;
		return this.hashCode() == object.hashCode();
	}

	@Override
	public String toString() {
		return StringUtils.defaultString(this.name, String.valueOf(this.id));
	}

}
