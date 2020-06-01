package org.ironrhino.core.elasticsearch.document;

import org.ironrhino.core.elasticsearch.Constants;
import org.ironrhino.core.elasticsearch.ElasticsearchEnabled;
import org.ironrhino.rest.client.RestApi;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@ElasticsearchEnabled
@RestApi(apiBaseUrl = Constants.VALUE_ELASTICSEARCH_URL, treatNotFoundAsNull = true, dateFormat = Constants.DATE_FORMAT)
public interface DocumentOperations<T> {

	@PostMapping("/{index}/_doc")
	IndexResult index(@PathVariable String index, @RequestBody T document);

	@PutMapping("/{index}/_doc/{id}")
	IndexResult index(@PathVariable String index, @PathVariable String id, @RequestBody T document);

	@PutMapping("/{index}/_create/{id}")
	IndexResult putIfAbsent(@PathVariable String index, @PathVariable String id, @RequestBody T document);

	@PostMapping("/{index}/_doc/{id}")
	IndexResult update(@PathVariable String index, @PathVariable String id, @RequestBody T document);

	@PostMapping("/{index}/_doc/{id}")
	IndexResult update(@PathVariable String index, @PathVariable String id, @RequestBody T document,
			@RequestParam("if_seq_no") int seqNo, @RequestParam("if_primary_term") int primaryTerm);

	@GetMapping("/{index}/_source/{id}")
	T get(@PathVariable String index, @PathVariable String id);

	@GetMapping("/{index}/_doc/{id}")
	Detail<T> detail(@PathVariable String index, @PathVariable String id);

	@DeleteMapping("/{index}/_doc/{id}")
	void delete(@PathVariable String index, @PathVariable String id);

}
