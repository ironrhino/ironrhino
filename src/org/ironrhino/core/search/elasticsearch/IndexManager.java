package org.ironrhino.core.search.elasticsearch;

import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.search.SearchHit;
import org.ironrhino.core.model.Persistable;

@SuppressWarnings("rawtypes")
public interface IndexManager {

	public String getIndexName();

	public Object searchHitToEntity(SearchHit sh) throws Exception;

	public ListenableActionFuture<IndexResponse> index(Persistable entity);

	public ListenableActionFuture<DeleteResponse> delete(Persistable entity);

	public void rebuild();

	public void indexAll(String type);

}
