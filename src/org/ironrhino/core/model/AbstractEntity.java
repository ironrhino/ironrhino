package org.ironrhino.core.model;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.NaturalId;
import org.ironrhino.core.util.AnnotationUtils;
import org.ironrhino.core.util.ReflectionUtils;

public abstract class AbstractEntity<PK extends Serializable> implements Persistable<PK> {

	private static final long serialVersionUID = 5366738895214161098L;

	@Override
	public int hashCode() {
		Map<String, Object> map = AnnotationUtils.getAnnotatedPropertyNameAndValues(this, NaturalId.class);
		HashCodeBuilder builder = new HashCodeBuilder();
		builder.append(this.getId());
		for (Object value : map.values())
			builder.append(value);
		return builder.toHashCode();
	}

	@Override
	@SuppressWarnings("rawtypes")
	public boolean equals(Object object) {
		if (object == null)
			return false;
		if (object == this)
			return true;
		if (!this.getClass().isAssignableFrom(object.getClass())
				&& !object.getClass().isAssignableFrom(this.getClass()))
			return false;
		AbstractEntity that = (AbstractEntity) object;
		return this.toIdentifiedString() != null && this.toIdentifiedString().equals(that.toIdentifiedString());
	}

	private String toIdentifiedString() {
		Map<String, Object> map = AnnotationUtils.getAnnotatedPropertyNameAndValues(this, NaturalId.class);
		if (map.size() == 1) {
			Object naturalId = map.values().iterator().next();
			if (naturalId != null)
				return String.valueOf(naturalId);
		} else if (map.size() > 1) {
			return ReflectionUtils.getActualClass(this).getName() + "{id=" + getId() + ",naturalId=" + map.toString()
					+ "}";
		}
		Object id = getId();
		return String.valueOf(id);
	}

	@Override
	public String toString() {
		return toIdentifiedString();
	}

}
