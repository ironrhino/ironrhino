package org.ironrhino.core.elasticsearch.search;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class SearchHit<T> {

	@JsonProperty("_score")
	private double score;

	@JsonProperty("_source")
	private T document;
	
}
