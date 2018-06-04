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
		if (this.getId() != null)
			return this.getId().equals(that.getId());
		if (that.getId() != null)
			return that.getId().equals(this.getId());
		Map<String, Object> thisNaturalIds = AnnotationUtils.getAnnotatedPropertyNameAndValues(this, NaturalId.class);
		if (!thisNaturalIds.isEmpty()) {
			Map<String, Object> thatNaturalIds = AnnotationUtils.getAnnotatedPropertyNameAndValues(that,
					NaturalId.class);
			for (Map.Entry<String, Object> entry : thisNaturalIds.entrySet()) {
				Object thisValue = entry.getValue();
				Object thatValue = thatNaturalIds.get(entry.getKey());
				if (thisValue == null || thatValue == null || !thisValue.equals(thatValue))
					return false;
			}
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		Map<String, Object> map = AnnotationUtils.getAnnotatedPropertyNameAndValues(this, NaturalId.class);
		if (map.size() == 1) {
			Object naturalId = map.values().iterator().next();
			if (naturalId != null)
				return String.valueOf(naturalId);
		} else if (map.size() > 1) {
			for (Object v : map.values())
				if (v != null)
					return map.toString();
		}
		return String.valueOf(getId());
	}

}
