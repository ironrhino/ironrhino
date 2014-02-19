package org.ironrhino.core.jdbc;

import java.io.Serializable;
import java.util.Map;

public class QueryCriteria implements Serializable {

	private static final long serialVersionUID = -2581363035277418165L;

	private String query;

	private Map<String, ?> parameters;

	public QueryCriteria() {

	}

	public QueryCriteria(String query, Map<String, ?> parameters) {
		this.query = query;
		this.parameters = parameters;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public Map<String, ?> getParameters() {
		return parameters;
	}

	public void setParameters(Map<String, ?> parameters) {
		this.parameters = parameters;
	}

	@Override
	public String toString() {
		return "QueryCriteria [query=" + query + ", parameters=" + parameters
				+ "]";
	}

}
