package org.ironrhino.core.elasticsearch.document;

import org.ironrhino.rest.client.RestApi;

@RestApi(apiBaseUrl = "${elasticsearch.url:http://localhost:9200}", treatNotFoundAsNull = true)
public interface ArticleOperations extends DocumentOperations<Article> {

}
