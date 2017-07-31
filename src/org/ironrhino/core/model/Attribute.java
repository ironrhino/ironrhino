package org.ironrhino.core.model;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.search.elasticsearch.annotations.Searchable;
import org.ironrhino.core.search.elasticsearch.annotations.SearchableProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Searchable(root = false)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Attribute implements Serializable {

	private static final long serialVersionUID = 3709022318256011161L;

	@SearchableProperty
	private String name;

	@SearchableProperty
	private String value;

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
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (StringUtils.isNotBlank(name) || StringUtils.isNotBlank(value)) {
			sb.append(name).append(" = ").append(StringUtils.isNotBlank(value) ? value : name);
		}
		return sb.toString();
	}

}
