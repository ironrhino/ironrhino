package org.ironrhino.core.elasticsearch;

import org.ironrhino.core.elasticsearch.document.DocumentOperations;
import org.ironrhino.core.elasticsearch.index.IndexOperations;
import org.ironrhino.core.elasticsearch.search.SearchOperations;

public interface ArticleOperations extends IndexOperations, DocumentOperations<Article>, SearchOperations<Article> {

}
