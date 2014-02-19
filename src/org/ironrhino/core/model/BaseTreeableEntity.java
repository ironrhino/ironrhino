package org.ironrhino.core.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.SequenceGenerator;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.metadata.CaseInsensitive;
import org.ironrhino.core.metadata.NotInCopy;
import org.ironrhino.core.metadata.NotInJson;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.search.elasticsearch.annotations.Index;
import org.ironrhino.core.search.elasticsearch.annotations.SearchableId;
import org.ironrhino.core.search.elasticsearch.annotations.SearchableProperty;

@SuppressWarnings({ "unchecked", "rawtypes" })
@MappedSuperclass
public class BaseTreeableEntity<T extends BaseTreeableEntity<T>> extends
		Entity<Long> implements Treeable<T>, Ordered {

	private static final long serialVersionUID = 2462271646391940930L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "treeable_entity_seq")
	@SequenceGenerator(name = "treeable_entity_seq", sequenceName = "treeable_entity_seq", allocationSize = 1)
	protected Long id;

	@Column(unique = false)
	@UiConfig(hidden = true)
	protected String fullId;

	@Column(nullable = false)
	@UiConfig(displayOrder = 1, width = "200px", cssClass = "checkavailable")
	@SearchableProperty
	protected String name;

	@Column(name = "`level`")
	@UiConfig(hidden = true)
	protected int level;

	@SearchableProperty
	@UiConfig(displayOrder = 1000, width = "100px")
	protected int displayOrder;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parentId", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
	protected T parent;

	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, mappedBy = "parent")
	@OrderBy("displayOrder,name")
	protected Collection<T> children = new HashSet<T>(0);

	@NotInJson
	public String getFullId() {
		return fullId;
	}

	public void setFullId(String fullId) {
		this.fullId = fullId;
	}

	@Override
	@SearchableId
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Override
	@NotInJson
	public boolean isNew() {
		return id == null || id == 0;
	}

	@Override
	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	@CaseInsensitive
	@SearchableProperty(boost = 3, index = Index.NOT_ANALYZED)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFullnameSeperator() {
		return "";
	}

	public String getFullname() {
		return getFullname(getFullnameSeperator());
	}

	public String getFullname(String seperator) {
		if (name == null)
			return null;
		StringBuilder fullname = new StringBuilder(name);
		BaseTreeableEntity e = this;
		while ((e = e.getParent()) != null) {
			if (!(e.isRoot() && StringUtils.isBlank(e.getName())))
				fullname.insert(0, e.getName() + seperator);
		}
		return fullname.toString();
	}

	@Override
	@NotInJson
	public int getDisplayOrder() {
		return displayOrder;
	}

	public void setDisplayOrder(int displayOrder) {
		this.displayOrder = displayOrder;
	}

	@Override
	public int compareTo(Object object) {
		if (!(object instanceof Ordered))
			return 0;
		Ordered ordered = (Ordered) object;
		if (this.getDisplayOrder() != ordered.getDisplayOrder())
			return this.getDisplayOrder() - ordered.getDisplayOrder();
		return this.toString().compareTo(ordered.toString());
	}

	@Override
	@NotInCopy
	@NotInJson
	@UiConfig(hidden = true)
	public Collection<T> getChildren() {
		return children;
	}

	public void setChildren(Collection<T> children) {
		this.children = children;
	}

	public boolean isLeaf() {
		return this.children == null || this.children.size() == 0;
	}

	public boolean isHasChildren() {
		return !isLeaf();
	}

	public boolean isRoot() {
		return this.parent == null;
	}

	@Override
	@NotInCopy
	@NotInJson
	@UiConfig(hidden = true)
	public T getParent() {
		return parent;
	}

	public void setParent(T parent) {
		this.parent = parent;
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

	@NotInJson
	public List<T> getDescendants() {
		List<T> ids = new ArrayList<T>();
		if (!this.isLeaf())
			for (Object obj : this.getChildren()) {
				collect((T) obj, ids);
			}
		return ids;
	}

	@NotInJson
	public List<T> getDescendantsAndSelf() {
		List<T> ids = new ArrayList<T>();
		collect((T) this, ids);
		return ids;
	}

	private void collect(T node, Collection<T> coll) {
		coll.add(node);
		if (node.isLeaf())
			return;
		for (Object obj : node.getChildren()) {
			collect((T) obj, coll);
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
		if (object == null || this.getClass() != object.getClass()
				|| this.hashCode() == 0 || object.hashCode() == 0)
			return false;
		return this.hashCode() == object.hashCode();
	}

	@Override
	public String toString() {
		return this.name;
	}
}
