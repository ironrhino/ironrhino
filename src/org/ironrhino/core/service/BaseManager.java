package org.ironrhino.core.service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.ironrhino.core.model.BaseTreeableEntity;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.model.ResultPage;
import org.springframework.orm.hibernate5.HibernateCallback;

public interface BaseManager<T extends Persistable<?>> {

	default DetachedCriteria detachedCriteria() {
		return DetachedCriteria.forClass(getEntityClass());
	}

	Class<? extends Persistable<?>> getEntityClass();

	void save(T obj);

	void update(T obj);

	T get(Serializable id);

	T getReference(Serializable id);

	boolean exists(Serializable id);

	T get(Serializable id, LockOptions lockOptions);

	List<T> get(List<? extends Serializable> ids);

	void evict(T obj);

	void refresh(T obj);

	void delete(T obj);

	List<T> delete(Serializable... id);

	long countByCriteria(DetachedCriteria dc);

	T findByCriteria(DetachedCriteria dc);

	List<T> findListByCriteria(DetachedCriteria dc);

	List<T> findBetweenListByCriteria(DetachedCriteria dc, int from, int end);

	List<T> findListByCriteria(DetachedCriteria dc, int pageNo, int pageSize);

	ResultPage<T> findByResultPage(ResultPage<T> resultPage);

	long countAll();

	T findByNaturalId(Serializable... objects);

	boolean existsNaturalId(Serializable... objects);

	T findOne(Serializable... objects);

	boolean existsOne(Serializable... objects);

	T findOne(boolean caseInsensitive, Serializable... objects);

	boolean existsOne(boolean caseInsensitive, Serializable... objects);

	List<T> findAll(Order... orders);

	List<T> find(String queryString, Object... args);

	List<T> find(String queryString, Map<String, ?> args);

	<TE extends BaseTreeableEntity<TE>> TE loadTree();

	int executeUpdate(String queryString, Object... args);

	int executeUpdate(String queryString, Map<String, ?> args);

	<K> K execute(HibernateCallback<K> callback);

	<K> K executeFind(HibernateCallback<K> callback);

	long iterate(int fetchSize, IterateCallback<T> callback);

	long iterate(int fetchSize, IterateCallback<T> callback, DetachedCriteria dc);

	long iterate(int fetchSize, IterateCallback<T> callback, DetachedCriteria dc, boolean commitPerFetch);

	long iterate(int fetchSize, IterateCallback<T> callback, Consumer<T[]> afterCommitConsumer, DetachedCriteria dc);

	@FunctionalInterface
	interface IterateCallback<T> {
		void process(T[] entityArray, Session session);
	}

}
