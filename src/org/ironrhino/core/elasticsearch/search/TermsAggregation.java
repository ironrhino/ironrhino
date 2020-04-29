package org.ironrhino.core.elasticsearch.search;

import java.util.Collections;
import java.util.Map;

public class TermsAggregation extends BucketAggregation {

	protected TermsAggregation(Map<String, Map<String, Map<String, Object>>> aggregations) {
		super(aggregations);
	}

	public static TermsAggregation of(String field) {
		return new TermsAggregation(Collections.singletonMap("aggs",
				Collections.singletonMap("terms", Collections.singletonMap("field", field))));
	}

}
