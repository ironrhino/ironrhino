package org.ironrhino.core.elasticsearch.search;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DateHistogramAggregation extends BucketAggregation {

	protected DateHistogramAggregation(Map<String, Map<String, Map<String, Object>>> aggregations) {
		super(aggregations);
	}

	public static DateHistogramAggregation of(String field, String intervalType, String intervalValue) {
		return of(field, intervalType, intervalValue, null);
	}

	public static DateHistogramAggregation of(String field, String intervalType, String intervalValue, String format) {
		Map<String, Object> map = new HashMap<>();
		map.put("field", field);
		map.put(intervalType, intervalValue);
		if (format != null)
			map.put("format", format);
		return new DateHistogramAggregation(
				Collections.singletonMap("aggs", Collections.singletonMap("date_histogram", map)));
	}

}
