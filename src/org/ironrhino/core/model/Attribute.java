package org.ironrhino.core.model;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.ironrhino.core.search.elasticsearch.annotations.Searchable;
import org.ironrhino.core.search.elasticsearch.annotations.SearchableProperty;

@Searchable(root = false)
public class Attribute implements Serializable {

	private static final long serialVersionUID = 3709022318256011161L;

	private String name;

	private String value;

	public Attribute() {
	}

	public Attribute(String name, String value) {
		this.name = name;
		this.value = value;
	}

	public Attribute(String name) {
		this.name = name;
	}

	@SearchableProperty
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@SearchableProperty
	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public boolean isNull() {
		return name == null && value == null;
	}

	public boolean isEmpty() {
		return StringUtils.isEmpty(name) && StringUtils.isEmpty(value);
	}

	public boolean isBlank() {
		return StringUtils.isBlank(name) && StringUtils.isBlank(value);
	}

	@Override
	public boolean equals(Object another) {
		return EqualsBuilder.reflectionEquals(this, another);
	}

	@Override
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (StringUtils.isNotBlank(name) || StringUtils.isNotBlank(value)) {
			sb.append(name).append(" = ").append(StringUtils.isNotBlank(value) ? value : name);
		}
		return sb.toString();
	}

}
