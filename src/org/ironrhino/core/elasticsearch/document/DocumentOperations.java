package org.ironrhino.core.elasticsearch.document;

import org.ironrhino.core.elasticsearch.Constants;
import org.ironrhino.rest.client.RestApi;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestApi(apiBaseUrl = Constants.ELASTICSEARCH_URL, treatNotFoundAsNull = true, dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS")
public interface DocumentOperations<T> {

	@PostMapping("/{index}/_doc")
	void index(@PathVariable String index, @RequestBody T document);

	@PutMapping("/{index}/_doc/{id}")
	void index(@PathVariable String index, @PathVariable String id, @RequestBody T document);

	@PutMapping("/{index}/_create/{id}")
	void putIfAbsent(@PathVariable String index, @PathVariable String id, @RequestBody T document);

	@PostMapping("/{index}/_doc/{id}")
	void update(@PathVariable String index, @PathVariable String id, @RequestBody T document);

	@GetMapping("/{index}/_source/{id}")
	T get(@PathVariable String index, @PathVariable String id);

	@DeleteMapping("/{index}/_doc/{id}")
	void delete(@PathVariable String index, @PathVariable String id);

}
