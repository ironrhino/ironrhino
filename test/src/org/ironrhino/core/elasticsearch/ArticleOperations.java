package org.ironrhino.core.elasticsearch;

import org.ironrhino.core.elasticsearch.document.DocumentOperations;
import org.ironrhino.core.elasticsearch.search.SearchOperations;

public interface ArticleOperations extends DocumentOperations<Article>, SearchOperations<Article> {

}
