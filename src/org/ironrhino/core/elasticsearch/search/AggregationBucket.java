package org.ironrhino.core.elasticsearch.search;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class AggregationBucket {

	private String key;

	@JsonProperty("doc_count")
	private int count;

	@JsonProperty("key_as_string")
	private String keyAsString;

}
