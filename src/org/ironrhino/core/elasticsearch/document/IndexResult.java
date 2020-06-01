package org.ironrhino.core.elasticsearch.document;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class IndexResult {

	@JsonProperty("_index")
	private String index;

	@JsonProperty("_id")
	private String id;

	@JsonProperty("_version")
	private int version;

	@JsonProperty("_seq_no")
	private int seqNo;

	@JsonProperty("_primary_term")
	private int primaryTerm;

}
