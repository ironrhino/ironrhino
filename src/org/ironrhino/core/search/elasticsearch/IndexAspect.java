package org.ironrhino.core.search.elasticsearch;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.ironrhino.core.aop.AopContext;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.search.elasticsearch.annotations.Searchable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;

@Aspect
@SuppressWarnings("rawtypes")
public class IndexAspect implements Ordered {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private IndexManager indexManager;

	private int order;

	public IndexAspect() {
		order = 1;
	}

	@AfterReturning(pointcut = "execution(java.util.List org.ironrhino.core.service.BaseManager.delete(*)) ", returning = "list")
	public void deleteBatch(List list) throws Throwable {
		if (!AopContext.isBypass(this.getClass()) && list != null)
			for (Object entity : list) {
				Searchable searchable = entity.getClass().getAnnotation(
						Searchable.class);
				if (searchable != null) {
					ListenableActionFuture<DeleteResponse> laf = indexManager
							.delete((Persistable) entity);
					laf.addListener(deleteResponseActionListener);
				}
			}
	}

	@AfterReturning("execution(* org.ironrhino.core.service.BaseManager.delete(*)) and args(entity) and @args(searchable)")
	public void delete(Persistable entity, Searchable searchable) {
		if (!AopContext.isBypass(this.getClass())) {
			ListenableActionFuture<DeleteResponse> laf = indexManager
					.delete(entity);
			laf.addListener(deleteResponseActionListener);
		}
	}

	@AfterReturning("execution(* org.ironrhino.core.service.BaseManager.save(*)) and args(entity) and @args(searchable)")
	public void save(Persistable entity, Searchable searchable)
			throws Throwable {
		if (!AopContext.isBypass(this.getClass())) {
			ListenableActionFuture<IndexResponse> laf = indexManager
					.index(entity);
			laf.addListener(indexResponseActionListener);
		}
	}

	@Override
	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	private ActionListener<DeleteResponse> deleteResponseActionListener = new ActionListener<DeleteResponse>() {
		@Override
		public void onResponse(DeleteResponse response) {
			if (!response.isFound())
				logger.warn("index is not found where deleting{} of {} ",
						response.getId(), response.getType());
		}

		@Override
		public void onFailure(Throwable e) {
			logger.error(e.getMessage(), e);
		}
	};

	private ActionListener<IndexResponse> indexResponseActionListener = new ActionListener<IndexResponse>() {
		@Override
		public void onResponse(IndexResponse response) {
		}

		@Override
		public void onFailure(Throwable e) {
			logger.error(e.getMessage(), e);
		}
	};

}
