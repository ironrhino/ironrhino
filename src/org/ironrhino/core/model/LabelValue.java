package org.ironrhino.core.model;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LabelValue implements Serializable {

	private static final long serialVersionUID = 7629652470042630809L;

	private String label;

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
