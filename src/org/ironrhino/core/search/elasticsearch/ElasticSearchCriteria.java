package org.ironrhino.core.search.elasticsearch;

import org.elasticsearch.index.query.QueryBuilder;
import org.ironrhino.core.search.SearchCriteria;

public class ElasticSearchCriteria extends SearchCriteria {

	private static final long serialVersionUID = 2810417180615970724L;

	private String[] indices;

	private String[] types;

	private QueryBuilder queryBuilder;

	public QueryBuilder getQueryBuilder() {
		return queryBuilder;
	}

	public void setQueryBuilder(QueryBuilder queryBuilder) {
		this.queryBuilder = queryBuilder;
	}

	public String[] getIndices() {
		return indices;
	}

	public void setIndices(String[] indices) {
		this.indices = indices;
	}

	public String[] getTypes() {
		return types;
	}

	public void setTypes(String[] types) {
		this.types = types;
	}

}
