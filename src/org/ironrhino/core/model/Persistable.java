package org.ironrhino.core.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface Persistable<PK extends Serializable> extends Serializable {

	@JsonIgnore
	public default boolean isNew() {
		PK id = getId();
		if (id instanceof String) {
			String str = (String) id;
			return str == null || str.isEmpty();
		} else if (id instanceof Number) {
			Number num = (Number) id;
			return num == null || num.longValue() <= 0;
		} else {
			return id == null;
		}
	}

	public PK getId();

}
