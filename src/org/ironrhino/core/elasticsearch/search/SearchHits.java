package org.ironrhino.core.elasticsearch.search;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class SearchHits<T> {

	@JsonProperty("max_score")
	private double maxScore;

	private Total total;

	private List<SearchHit<T>> hits;

}
