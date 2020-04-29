package org.ironrhino.core.elasticsearch.search;

import java.util.Collections;
import java.util.Map;

import lombok.Value;

@Value
public class TermsAggregation {

	private int size;

	private Map<String, Map<String, Map<String, String>>> aggregations;

	public static TermsAggregation of(String field) {
		return new TermsAggregation(0, Collections.singletonMap("aggs",
				Collections.singletonMap("terms", Collections.singletonMap("field", field))));
	}

}
