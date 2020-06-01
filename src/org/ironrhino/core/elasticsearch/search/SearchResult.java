package org.ironrhino.core.elasticsearch.search;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class SearchResult<T> {

	@JsonProperty("_scroll_id")
	private String scrollId;

	private long took;

	@JsonProperty("timed_out")
	private boolean timedOut;

	private SearchHits<T> hits;

}
