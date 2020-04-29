package org.ironrhino.core.elasticsearch.search;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class HistogramAggregation extends BucketAggregation {

	protected HistogramAggregation(Map<String, Map<String, Map<String, Object>>> aggregations) {
		super(aggregations);
	}

	public static HistogramAggregation of(String field, int interval) {
		Map<String, Object> map = new HashMap<>();
		map.put("field", field);
		map.put("interval", interval);
		return new HistogramAggregation(Collections.singletonMap("aggs", Collections.singletonMap("histogram", map)));
	}

}
