package org.ironrhino.core.elasticsearch.search;

import java.util.List;

import org.ironrhino.core.elasticsearch.Constants;
import org.ironrhino.rest.client.JsonPointer;
import org.ironrhino.rest.client.RestApi;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@RestApi(apiBaseUrl = Constants.ELASTICSEARCH_URL, treatNotFoundAsNull = true, dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS")
public interface SearchOperations<T> {

	@GetMapping("/{index}/_search")
	@JsonPointer("/hits/hits")
	List<SearchHits<T>> search(@PathVariable String index, @RequestParam("q") String query);

	@GetMapping("/{index}/_search")
	@JsonPointer("/hits/hits")
	List<SearchHits<T>> search(@PathVariable String index, @RequestParam("q") String query, @RequestParam int from,
			@RequestParam int size);

	@PostMapping("/{index}/_search?size=0")
	@JsonPointer("/aggregations/aggs/buckets")
	List<AggregationBucket> aggregate(@PathVariable String index, @RequestBody TermsAggregation aggregation);

	@PostMapping("/{index}/_search?size=0")
	@JsonPointer("/aggregations/aggs/buckets")
	List<AggregationBucket> aggregate(@PathVariable String index, @RequestBody HistogramAggregation aggregation);

	@PostMapping("/{index}/_search?size=0")
	@JsonPointer("/aggregations/aggs/buckets")
	List<AggregationBucket> aggregate(@PathVariable String index, @RequestBody DateHistogramAggregation aggregation);

}
