package org.ironrhino.core.elasticsearch.index;

import org.ironrhino.core.elasticsearch.Constants;
import org.ironrhino.core.elasticsearch.ElasticsearchEnabled;
import org.ironrhino.rest.client.JsonPointer;
import org.ironrhino.rest.client.RestApi;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@ElasticsearchEnabled
@RestApi(apiBaseUrl = Constants.VALUE_ELASTICSEARCH_URL, treatNotFoundAsNull = true, dateFormat = Constants.DATE_FORMAT)
public interface IndexOperations {

	@PutMapping("/{index}")
	void create(@PathVariable String index);

	@PutMapping("/{index}")
	void create(@PathVariable String index, @RequestBody Configuration configuration);

	@RequestMapping(value = "/{index}", method = RequestMethod.HEAD)
	boolean exists(@PathVariable String index);

	@DeleteMapping("/{index}")
	void delete(@PathVariable String index);

	@PostMapping("/{index}/_clone/${targetIndex}")
	void clone(@PathVariable String index, @PathVariable String targetIndex);

	@PostMapping("/{index}/_close")
	void close(@PathVariable String index);

	@PostMapping("/{index}/_refresh")
	void refresh(@PathVariable String index);

	@PostMapping("/{index}/_flush")
	void flush(@PathVariable String index);

	@PostMapping("/{index}/_freeze")
	void freeze(@PathVariable String index);

	@PutMapping("/{index}/_alias/{alias}")
	void addAlias(@PathVariable String index, @PathVariable String alias);

	@DeleteMapping("/{index}/_alias/{alias}")
	void deleteAlias(@PathVariable String index, @PathVariable String alias);

	@GetMapping("/{index}/_mapping")
	@JsonPointer("/${index}/mappings")
	Mappings getMapping(@PathVariable String index);

	@PutMapping("/{index}/_mapping")
	void putMapping(@PathVariable String index, @RequestBody Mappings mappings);

}
