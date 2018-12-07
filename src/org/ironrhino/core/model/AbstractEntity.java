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
		Map<String, Object> naturalIds = AnnotationUtils.getAnnotatedPropertyNameAndValues(this, NaturalId.class);
		if (!naturalIds.isEmpty()) {
			for (Object value : naturalIds.values())
				builder.append(value);
			return builder.toHashCode();
		} else {
			return 31;
		}
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
		Map<String, Object> naturalIds = AnnotationUtils.getAnnotatedPropertyNameAndValues(this, NaturalId.class);
		if (!naturalIds.isEmpty()) {
			Map<String, Object> thatNaturalIds = AnnotationUtils.getAnnotatedPropertyNameAndValues(object,
					NaturalId.class);
			for (Map.Entry<String, Object> entry : naturalIds.entrySet()) {
				Object thisValue = entry.getValue();
				Object thatValue = thatNaturalIds.get(entry.getKey());
				if (thisValue == null || thatValue == null || !thisValue.equals(thatValue))
					return false;
			}
			return true;
		} else {
			return this.getId() != null && this.getId().equals(((AbstractEntity) object).getId());
		}
	}

	@Override
	public String toString() {
		Map<String, Object> naturalIds = AnnotationUtils.getAnnotatedPropertyNameAndValues(this, NaturalId.class);
		if (naturalIds.size() == 1) {
			return String.valueOf(naturalIds.values().iterator().next());
		} else if (naturalIds.size() > 1) {
			return getClass().getSimpleName() + '@' + String.valueOf(naturalIds);
		} else {
			return getClass().getSimpleName() + '@' + String.valueOf(getId());
		}
	}

}
