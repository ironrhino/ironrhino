package org.ironrhino.core.elasticsearch.search;

import java.util.Collections;
import java.util.Map;

import lombok.Value;

@Value
public class TermsAggregation {

	private Map<String, Map<String, Map<String, Object>>> aggregations;

	public static TermsAggregation of(String field) {
		return new TermsAggregation(Collections.singletonMap("aggs",
				Collections.singletonMap("terms", Collections.singletonMap("field", field))));
	}

}
