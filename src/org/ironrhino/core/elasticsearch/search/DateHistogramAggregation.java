package org.ironrhino.core.elasticsearch.search;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import lombok.Value;

@Value
public class DateHistogramAggregation {

	private int size;

	private Map<String, Map<String, Map<String, String>>> aggregations;

	public static DateHistogramAggregation of(String field, String intervalType, String intervalValue) {
		Map<String,String> map = new HashMap<>();
		map.put("field", field);
		map.put(intervalType, intervalValue);
		return new DateHistogramAggregation(0, Collections.singletonMap("aggs",
				Collections.singletonMap("date_histogram", map)));
	}

}
