package org.ironrhino.core.elasticsearch.document;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Detail<T> extends IndexResult {

	@JsonProperty("_source")
	private T source;

}
