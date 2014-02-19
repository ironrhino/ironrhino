package org.ironrhino.core.search;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public class SearchCriteria implements Serializable {

	private static final long serialVersionUID = 2268675250220590326L;

	private String query;

	private Map<String, Boolean> sorts = new LinkedHashMap<String, Boolean>(4,1);

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public Map<String, Boolean> getSorts() {
		return sorts;
	}

	public void addSort(String name, boolean desc) {
		sorts.put(name, desc);
	}

}
