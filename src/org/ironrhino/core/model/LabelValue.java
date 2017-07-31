package org.ironrhino.core.model;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.search.elasticsearch.annotations.Searchable;
import org.ironrhino.core.search.elasticsearch.annotations.SearchableProperty;

import lombok.Data;
import lombok.NoArgsConstructor;

@Searchable(root = false)
@Data
@NoArgsConstructor
public class LabelValue implements Serializable {

	private static final long serialVersionUID = 7629652470042630809L;

	@SearchableProperty(boost = 2)
	private String label;

	@SearchableProperty(boost = 2)
	private String value;

	private Boolean selected;

	public LabelValue(String label, String value) {
		this.label = label;
		this.value = value;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (StringUtils.isNotBlank(label) || StringUtils.isNotBlank(value)) {
			sb.append(value).append(" = ").append(StringUtils.isNotBlank(label) ? label : value);
		}
		return sb.toString();
	}

}
