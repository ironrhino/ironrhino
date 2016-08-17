package org.ironrhino.core.model;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.NaturalId;
import org.ironrhino.core.util.AnnotationUtils;

public abstract class AbstractEntity<PK extends Serializable> implements Persistable<PK> {

	private static final long serialVersionUID = 5366738895214161098L;

	@Override
	public int hashCode() {
		HashCodeBuilder builder = new HashCodeBuilder();
		Map<String, Object> map = AnnotationUtils.getAnnotatedPropertyNameAndValues(this, NaturalId.class);
		if (!map.isEmpty()) {
			for (Object value : map.values())
				builder.append(value);
		} else {
			builder.append(this.getId());
		}
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
		return this.toIdentifiedString().equals(that.toIdentifiedString());
	}

	private String toIdentifiedString() {
		Map<String, Object> map = AnnotationUtils.getAnnotatedPropertyNameAndValues(this, NaturalId.class);
		if (map.size() == 1) {
			return String.valueOf(map.values().iterator().next());
		} else if (map.size() > 1) {
			return map.toString();
		} else {
			return String.valueOf(getId());
		}
	}

	@Override
	public String toString() {
		return toIdentifiedString();
	}

}
