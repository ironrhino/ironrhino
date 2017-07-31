package org.ironrhino.core.search;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
public class SearchCriteria implements Serializable {

	private static final long serialVersionUID = 2268675250220590326L;

	@Setter
	private String[] types;

	@Setter
	private String query;

	private Map<String, Boolean> sorts = new LinkedHashMap<>(4, 1);

	public void addSort(String name, boolean desc) {
		sorts.put(name, desc);
	}

}
