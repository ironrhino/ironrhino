package org.ironrhino.core.elasticsearch.search;

import java.util.Map;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public abstract class BucketAggregation {

	private final Map<String, Map<String, Map<String, Object>>> aggregations;

}
