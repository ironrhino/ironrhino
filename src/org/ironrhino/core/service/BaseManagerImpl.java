package org.ironrhino.core.service;

import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.LockOptions;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.NaturalId;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.internal.CriteriaImpl;
import org.hibernate.internal.CriteriaImpl.OrderEntry;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.hibernate.transform.ResultTransformer;
import org.ironrhino.core.metadata.AppendOnly;
import org.ironrhino.core.model.BaseTreeableEntity;
import org.ironrhino.core.model.Ordered;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.model.ResultPage;
import org.ironrhino.core.struts.EntityClassHelper;
import org.ironrhino.core.util.AnnotationUtils;
import org.ironrhino.core.util.BeanUtils;
import org.ironrhino.core.util.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate5.HibernateCallback;
import org.springframework.transaction.annotation.Transactional;

@SuppressWarnings({ "unchecked", "rawtypes" })
public abstract class BaseManagerImpl<T extends Persistable<?>> implements BaseManager<T> {

	protected Logger logger = LoggerFactory.getLogger(getClass());

	private Class<T> entityClass;

	@Autowired
	protected SessionFactory sessionFactory;

	@Autowired
	private DeleteChecker deleteChecker;

	private static final MethodHandle CRITERIA_IMPL_GETTER;

	static {
		try {
			Field f = DetachedCriteria.class.getDeclaredField("impl");
			f.setAccessible(true);
			CRITERIA_IMPL_GETTER = MethodHandles.lookup().unreflectGetter(f);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	};

	public BaseManagerImpl() {
		Class<T> clazz = (Class<T>) ReflectionUtils.getGenericClass(getClass());
		if (clazz != null)
			entityClass = clazz;
	}

	public BaseManagerImpl(Class<T> clazz) {
		entityClass = clazz;
	}

	@Override
	public Class<T> getEntityClass() {
		return entityClass;
	}

	@Override
	@Transactional
	public void save(T obj) {
		Class<?> clazz = ReflectionUtils.getActualClass(obj);
		Immutable immutable = clazz.getAnnotation(Immutable.class);
		if (immutable != null)
			throw new IllegalArgumentException(clazz + " is @" + Immutable.class.getSimpleName());
		AppendOnly appendOnly = clazz.getAnnotation(AppendOnly.class);
		boolean isnew = obj.isNew();
		if (EntityClassHelper.isIdAssigned(clazz)) {
			Serializable id = obj.getId();
			if (id == null)
				throw new IllegalArgumentException(obj + " must have an ID");
			if (appendOnly == null) {
				DetachedCriteria dc = detachedCriteria();
				dc.add(Restrictions.eq("id", id));
				isnew = countByCriteria(dc) == 0;
			} else {
				isnew = true;
			}
		} else {
			if (appendOnly != null && !isnew)
				throw new IllegalArgumentException(clazz + " is @" + AppendOnly.class.getSimpleName());
		}
		ReflectionUtils.processCallback(obj, isnew ? PrePersist.class : PreUpdate.class);
		Session session = sessionFactory.getCurrentSession();
		if (obj instanceof BaseTreeableEntity) {
			final BaseTreeableEntity entity = (BaseTreeableEntity) obj;
			boolean childrenNeedChange = false;
			if (entity.isNew()) {
				FlushMode mode = session.getFlushMode();
				session.setFlushMode(FlushMode.MANUAL);
				entity.setFullId("");
				session.save(entity);
				session.flush();
				session.setFlushMode(mode);
			} else {
				childrenNeedChange = (entity.getParent() == null && entity.getLevel() != 1
						|| entity.getParent() != null && (entity.getLevel() - entity.getParent().getLevel() != 1
								|| !entity.getFullId().startsWith(entity.getParent().getFullId())))
						&& entity.isHasChildren();

			}
			String fullId = String.valueOf(entity.getId()) + ".";
			if (entity.getParent() != null)
				fullId = entity.getParent().getFullId() + fullId;
			entity.setFullId(fullId);
			entity.setLevel(fullId.split("\\.").length);
			session.saveOrUpdate(obj);
			if (childrenNeedChange) {
				for (Object c : entity.getChildren()) {
					save((T) c);
				}
			}
		} else {
			if (isnew)
				session.save(obj);
			else
				session.update(obj);
		}
		ReflectionUtils.processCallback(obj, isnew ? PostPersist.class : PostUpdate.class);
	}

	@Override
	@Transactional
	public void update(T obj) {
		Class<?> clazz = ReflectionUtils.getActualClass(obj);
		Immutable immutable = clazz.getAnnotation(Immutable.class);
		if (immutable != null)
			throw new IllegalArgumentException(clazz + " is @" + Immutable.class.getSimpleName());
		AppendOnly appendOnly = clazz.getAnnotation(AppendOnly.class);
		if (appendOnly != null)
			throw new IllegalArgumentException(clazz + " is @" + AppendOnly.class.getSimpleName());
		if (obj.isNew())
			throw new IllegalArgumentException(obj + " must be persisted before update");
		ReflectionUtils.processCallback(obj, PreUpdate.class);
		sessionFactory.getCurrentSession().update(obj);
		ReflectionUtils.processCallback(obj, PostUpdate.class);
	}

	@Override
	@Transactional
	public void delete(T obj) {
		Class<?> clazz = ReflectionUtils.getActualClass(obj);
		Immutable immutable = clazz.getAnnotation(Immutable.class);
		if (immutable != null)
			throw new IllegalArgumentException(clazz + " is @" + Immutable.class.getSimpleName());
		AppendOnly appendOnly = clazz.getAnnotation(AppendOnly.class);
		if (appendOnly != null)
			throw new IllegalArgumentException(clazz + " is @" + AppendOnly.class.getSimpleName());
		checkDelete(obj);
		ReflectionUtils.processCallback(obj, PreRemove.class);
		sessionFactory.getCurrentSession().delete(obj);
		ReflectionUtils.processCallback(obj, PostRemove.class);
	}

	protected void checkDelete(T obj) {
		Class<?> clazz = ReflectionUtils.getActualClass(obj);
		Immutable immutable = clazz.getAnnotation(Immutable.class);
		if (immutable != null)
			throw new IllegalArgumentException(clazz + " is @" + Immutable.class.getSimpleName());
		AppendOnly appendOnly = clazz.getAnnotation(AppendOnly.class);
		if (appendOnly != null)
			throw new IllegalArgumentException(clazz + " is @" + AppendOnly.class.getSimpleName());
		deleteChecker.check(obj);
	}

	@Override
	@Transactional
	public List<T> delete(Serializable... id) {
		Immutable immutable = getEntityClass().getAnnotation(Immutable.class);
		if (immutable != null)
			throw new IllegalArgumentException(getEntityClass() + " is @" + Immutable.class.getSimpleName());
		AppendOnly appendOnly = getEntityClass().getAnnotation(AppendOnly.class);
		if (appendOnly != null)
			throw new IllegalArgumentException(getEntityClass() + " is @" + AppendOnly.class.getSimpleName());
		if (id == null || id.length == 0 || id.length == 1 && id[0] == null)
			return null;
		if (id.length == 1 && id[0].getClass().isArray()) {
			Object[] objs = (Object[]) id[0];
			Serializable[] arr = new Serializable[objs.length];
			for (int i = 0; i < objs.length; i++)
				arr[i] = (Serializable) objs[i];
			id = arr;
		}
		Class idtype = String.class;
		BeanWrapperImpl bw = null;
		try {
			bw = new BeanWrapperImpl(getEntityClass().newInstance());
			idtype = getEntityClass().getMethod("getId", new Class[0]).getReturnType();
		} catch (Exception e) {
		}
		Serializable[] arr = new Serializable[id.length];
		for (int i = 0; i < id.length; i++) {
			Serializable s = id[i];
			if (!s.getClass().equals(idtype)) {
				bw.setPropertyValue("id", s);
				arr[i] = (Serializable) bw.getPropertyValue("id");
			} else {
				arr[i] = s;
			}
		}
		id = arr;
		List<T> list;
		if (id.length == 1) {
			list = new ArrayList<>(1);
			list.add(get(id[0]));
		} else {
			DetachedCriteria dc = detachedCriteria();
			dc.add(Restrictions.in("id", (Object[]) id));
			list = findListByCriteria(dc);
		}
		if (list.size() > 0) {
			for (final T obj : list)
				checkDelete(obj);
			for (T obj : list)
				delete(obj);
		}
		return list;
	}

	@Override
	@Transactional(readOnly = true)
	public T get(Serializable id) {
		if (id == null)
			return null;
		return sessionFactory.getCurrentSession().get(getEntityClass(), id);
	}

	@Override
	@Transactional
	public T get(Serializable id, LockOptions lockOptions) {
		if (id == null)
			return null;
		return sessionFactory.getCurrentSession().get(getEntityClass(), id, lockOptions);
	}

	@Override
	@Transactional(readOnly = true)
	public List<T> get(List<? extends Serializable> ids) {
		if (ids == null)
			return null;
		if (ids.isEmpty())
			return Collections.emptyList();
		if (ids.size() > ResultPage.DEFAULT_MAX_PAGESIZE)
			throw new IllegalArgumentException("ids size shouldn't large than " + ResultPage.DEFAULT_MAX_PAGESIZE);
		return sessionFactory.getCurrentSession().byMultipleIds(getEntityClass()).multiLoad(ids);
	}

	@Override
	public void evict(T obj) {
		if (obj != null) {
			if (obj instanceof BaseTreeableEntity) {
				BaseTreeableEntity te = (BaseTreeableEntity) obj;
				Hibernate.initialize(te.getChildren());
				if (te.getChildren() != null) {
					for (Object child : te.getChildren())
						sessionFactory.getCurrentSession().evict(child);
				}
			}
			sessionFactory.getCurrentSession().evict(obj);
		}
	}

	@Override
	public DetachedCriteria detachedCriteria() {
		return DetachedCriteria.forClass(getEntityClass());
	}

	@Override
	@Transactional(readOnly = true)
	public long countByCriteria(DetachedCriteria dc) {
		CriteriaImpl impl;
		try {
			impl = (CriteriaImpl) CRITERIA_IMPL_GETTER.invokeExact(dc);
		} catch (Throwable e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		ResultTransformer rt = impl.getResultTransformer();
		Iterator<OrderEntry> it = impl.iterateOrderings();
		List<OrderEntry> orderEntries = null;
		boolean notEmpty = it.hasNext();
		if (notEmpty) {
			// remove order
			orderEntries = new ArrayList<>();
			while (it.hasNext()) {
				orderEntries.add(it.next());
				it.remove();
			}
		}
		Criteria c = dc.getExecutableCriteria(sessionFactory.getCurrentSession());
		c.setProjection(Projections.projectionList().add(Projections.rowCount()));
		long count = (Long) c.uniqueResult();
		if (notEmpty) {
			// restore order
			for (OrderEntry oe : orderEntries)
				impl.addOrder(oe.getOrder());
		}
		dc.setProjection(null);
		dc.setResultTransformer(rt);
		return count;
	}

	@Override
	@Transactional(readOnly = true)
	public T findByCriteria(DetachedCriteria dc) {
		Criteria c = dc.getExecutableCriteria(sessionFactory.getCurrentSession());
		c.setMaxResults(1);
		c.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
		return (T) c.uniqueResult();
	}

	@Override
	@Transactional(readOnly = true)
	public List<T> findListByCriteria(DetachedCriteria dc) {
		Criteria c = dc.getExecutableCriteria(sessionFactory.getCurrentSession());
		c.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
		return c.list();
	}

	@Override
	@Transactional(readOnly = true)
	public List<T> findBetweenListByCriteria(DetachedCriteria dc, int start, int end) {
		Criteria c = dc.getExecutableCriteria(sessionFactory.getCurrentSession());
		if (!(start == 0 && end == Integer.MAX_VALUE)) {
			int firstResult = start;
			if (firstResult < 0)
				firstResult = 0;
			c.setFirstResult(firstResult);
			int maxResults = end - firstResult;
			if (maxResults > 0)
				c.setMaxResults(maxResults);
		}
		return c.list();
	}

	@Override
	@Transactional(readOnly = true)
	public List<T> findListByCriteria(DetachedCriteria dc, int pageNo, int pageSize) {
		return findBetweenListByCriteria(dc, (pageNo - 1) * pageSize, pageNo * pageSize);
	}

	@Override
	@Transactional(readOnly = true)
	public ResultPage<T> findByResultPage(ResultPage<T> resultPage) {
		DetachedCriteria detachedCriteria = resultPage.getCriteria();
		if (detachedCriteria == null)
			detachedCriteria = detachedCriteria();
		long totalResults = -1;
		if (resultPage.isCounting()) {
			totalResults = countByCriteria(detachedCriteria);
			resultPage.setTotalResults(totalResults);
			if (resultPage.getPageNo() < 1)
				resultPage.setPageNo(1);
			else if (resultPage.getPageNo() > resultPage.getTotalPage()) {
				// resultPage.setPageNo(resultPage.getTotalPage());
				resultPage.setResult(Collections.EMPTY_LIST);
				return resultPage;
			}
		}
		long time = System.currentTimeMillis();
		if (resultPage.isPaginating()) {
			int start, end;
			if (!resultPage.isReverse()) {
				start = (resultPage.getPageNo() - 1) * resultPage.getPageSize();
				end = resultPage.getPageNo() * resultPage.getPageSize();
			} else {
				start = (int) (resultPage.getTotalResults() - resultPage.getPageNo() * resultPage.getPageSize());
				end = (int) (resultPage.getTotalResults() - (resultPage.getPageNo() - 1) * resultPage.getPageSize());
			}
			if (!(resultPage.isCounting() && totalResults == 0))
				resultPage.setResult(findBetweenListByCriteria(detachedCriteria, start, end));
			else
				resultPage.setResult(Collections.EMPTY_LIST);
			resultPage.setStart(start);
		} else {
			resultPage.setResult(findListByCriteria(detachedCriteria));
		}
		resultPage.setTookInMillis(System.currentTimeMillis() - time);
		return resultPage;
	}

	@Override
	@Transactional(readOnly = true)
	public long countAll() {
		return countByCriteria(detachedCriteria());
	}

	@Override
	@Transactional(readOnly = true)
	public List<T> findAll(Order... orders) {
		Criteria c = sessionFactory.getCurrentSession().createCriteria(getEntityClass());
		c.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
		if (orders.length == 0) {
			if (Ordered.class.isAssignableFrom(getEntityClass()))
				c.addOrder(Order.asc("displayOrder"));
		} else
			for (Order order : orders)
				c.addOrder(order);
		return c.list();
	}

	@Override
	@Transactional(readOnly = true)
	public T findByNaturalId(Serializable... objects) {
		if (objects == null || objects.length == 0 || objects.length == 1 && objects[0] == null)
			return null;
		if (objects.length == 1 && objects[0].getClass().isArray()) {
			Object[] objs = (Object[]) objects[0];
			Serializable[] arr = new Serializable[objs.length];
			for (int i = 0; i < objs.length; i++)
				arr[i] = (Serializable) objs[i];
			objects = arr;
		}
		Criteria c = sessionFactory.getCurrentSession().createCriteria(getEntityClass());
		if (objects.length == 1) {
			Set<String> naturalIds = AnnotationUtils.getAnnotatedPropertyNames(getEntityClass(), NaturalId.class);
			if (naturalIds.size() != 1)
				throw new IllegalArgumentException("@NaturalId must and only be one");
			c.add(Restrictions.eq(naturalIds.iterator().next(), objects[0]));
		} else {
			if (objects.length == 0 || objects.length % 2 != 0)
				throw new IllegalArgumentException("parameter size must be even");
			int doubles = objects.length / 2;
			for (int i = 0; i < doubles; i++)
				c.add(Restrictions.eq(String.valueOf(objects[2 * i]), objects[2 * i + 1]));
		}
		c.setMaxResults(1);
		return (T) c.uniqueResult();
	}

	@Override
	@Transactional(readOnly = true)
	public T findOne(Serializable... objects) {
		if (objects == null || objects.length == 0 || objects.length == 1 && objects[0] == null)
			return null;
		for (Serializable ser : objects) {
			if (ser == null || ser instanceof Persistable && ((Persistable) ser).isNew())
				return null;
		}
		if (objects.length == 1 && objects[0].getClass().isArray()) {
			Object[] objs = (Object[]) objects[0];
			Serializable[] arr = new Serializable[objs.length];
			for (int i = 0; i < objs.length; i++)
				arr[i] = (Serializable) objs[i];
			objects = arr;
		}
		Criteria c = sessionFactory.getCurrentSession().createCriteria(getEntityClass());
		if (objects.length == 1) {
			Set<String> naturalIds = AnnotationUtils.getAnnotatedPropertyNames(getEntityClass(), NaturalId.class);
			if (naturalIds.size() != 1)
				throw new IllegalArgumentException("@NaturalId must and only be one");
			c.add(Restrictions.eq(naturalIds.iterator().next(), objects[0]));
		} else {
			if (objects.length == 0 || objects.length % 2 != 0)
				throw new IllegalArgumentException("parameter size must be even");
			int doubles = objects.length / 2;
			for (int i = 0; i < doubles; i++)
				c.add(Restrictions.eq(String.valueOf(objects[2 * i]), objects[2 * i + 1]));
		}
		c.setMaxResults(1);
		return (T) c.uniqueResult();
	}

	@Override
	@Transactional(readOnly = true)
	public T findOne(boolean caseInsensitive, Serializable... objects) {
		if (!caseInsensitive)
			return findOne(objects);
		for (Serializable ser : objects)
			if (ser == null || ser instanceof Persistable && ((Persistable) ser).isNew())
				return null;
		String hql = "select entity from " + getEntityClass().getName() + " entity where ";
		Query query;
		if (objects.length == 1) {
			Set<String> naturalIds = AnnotationUtils.getAnnotatedPropertyNames(getEntityClass(), NaturalId.class);
			if (naturalIds.size() != 1)
				throw new IllegalArgumentException("@NaturalId must and only be one");
			hql += "lower(entity." + naturalIds.iterator().next() + ")=lower(?1)";
			query = sessionFactory.getCurrentSession().createQuery(hql);
			query.setParameter("1", objects[0]);
		} else {
			if (objects.length == 0 || objects.length % 2 != 0)
				throw new IllegalArgumentException("parameter size must be even");
			int doubles = objects.length / 2;
			if (doubles == 1) {
				hql += "lower(entity." + String.valueOf(objects[0]) + ")=lower(?1)";
			} else {
				List<String> list = new ArrayList<>(doubles);
				for (int i = 0; i < doubles; i++)
					list.add("lower(entity." + String.valueOf(objects[2 * i]) + ")=lower(?" + (i + 1) + ")");
				hql += StringUtils.join(list, " and ");
			}
			query = sessionFactory.getCurrentSession().createQuery(hql);
			for (int i = 0; i < doubles; i++)
				query.setParameter(String.valueOf(i + 1), objects[2 * i + 1]);
		}
		query.setMaxResults(1);
		return (T) query.uniqueResult();
	}

	@Override
	@Transactional(readOnly = true)
	public List<T> find(final String queryString, final Object... args) {
		Query query = sessionFactory.getCurrentSession().createQuery(queryString);
		for (int i = 0; i < args.length; i++)
			query.setParameter(String.valueOf(i + 1), args[i]);
		return query.list();
	}

	@Override
	@Transactional(readOnly = true)
	public <TE extends BaseTreeableEntity<TE>> TE loadTree() {
		if (getEntityClass() == null || !(BaseTreeableEntity.class.isAssignableFrom(getEntityClass())))
			throw new IllegalArgumentException(
					"entityClass mustn't be null,and must extends class 'BaseTreeableEntity'");
		try {
			TE root = (TE) getEntityClass().newInstance();
			root.setId(0L);
			root.setName("");
			assemble(root, (List<TE>) findAll(Order.asc("level")));
			return root;
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	private <TE extends BaseTreeableEntity<TE>> void assemble(TE te, List<TE> list)
			throws InstantiationException, IllegalAccessException {
		List<TE> children = new ArrayList<>();
		Iterator<TE> it = list.iterator();
		while (it.hasNext()) {
			TE r = it.next();
			// already order by level asc
			if (r.getLevel() <= te.getLevel())
				continue;
			if (r.getLevel() - te.getLevel() > 1)
				break;
			boolean isChild = false;
			if (te.getId() == null && StringUtils.isNotBlank(te.getFullId())) {
				// workaround for javassist-3.16.x
				String fullId = te.getFullId();
				if (fullId.endsWith("."))
					fullId = fullId.substring(0, fullId.length() - 1);
				String id = fullId.substring(fullId.lastIndexOf('.') + 1);
				te.setId(Long.valueOf(id));
			}
			String fullId = r.getFullId();
			if (fullId.endsWith("."))
				fullId = fullId.substring(0, fullId.length() - 1);
			if (te.getId() == 0) {
				if (fullId.indexOf('.') < 0)
					isChild = true;
			} else {
				if (fullId.indexOf('.') > 0 && te.getFullId().equals(fullId.substring(0, fullId.lastIndexOf('.') + 1)))
					isChild = true;
			}
			if (isChild) {
				it.remove();
				TE rr = (TE) te.getClass().newInstance();
				BeanUtils.copyProperties(r, rr);
				children.add(rr);
				rr.setParent(te);
			}
		}
		Collections.sort(children);
		te.setChildren(children);
		for (TE r : children)
			assemble(r, list);
	}

	@Override
	@Transactional
	public int executeUpdate(String queryString, Object... values) {
		Query queryObject = sessionFactory.getCurrentSession().createQuery(queryString);
		for (int i = 0; i < values.length; i++) {
			queryObject.setParameter(String.valueOf(i + 1), values[i]);
		}
		return queryObject.executeUpdate();
	}

	@Override
	@Transactional(readOnly = true)
	@Deprecated
	public List<T> executeQuery(String queryString, Object... args) {
		return find(queryString, args);
	}

	@Override
	@Transactional
	public <K> K execute(HibernateCallback<K> callback) {
		return callback.doInHibernate(sessionFactory.getCurrentSession());
	}

	@Override
	@Transactional(readOnly = true)
	public <K> K executeFind(HibernateCallback<K> callback) {
		return execute(callback);
	}

	@Override
	public long iterate(int fetchSize, IterateCallback callback) {
		return iterate(fetchSize, callback, null);
	}

	@Override
	public long iterate(int fetchSize, IterateCallback callback, DetachedCriteria dc) {
		return iterate(fetchSize, callback, dc, false);
	}

	@Override
	public long iterate(int fetchSize, IterateCallback callback, DetachedCriteria dc, boolean commitPerFetch) {
		Session iterateSession = sessionFactory.openSession();
		iterateSession.setCacheMode(CacheMode.IGNORE);
		Session callbackSession = commitPerFetch ? sessionFactory.openSession() : iterateSession;
		if (dc == null) {
			dc = detachedCriteria();
			dc.addOrder(Order.asc("id"));
		}
		Criteria c = dc.getExecutableCriteria(iterateSession);
		c.setFetchSize(fetchSize);
		ScrollableResults cursor = null;
		Transaction transaction = null;
		long count = 0;
		try {
			transaction = callbackSession.beginTransaction();
			cursor = c.scroll(ScrollMode.FORWARD_ONLY);
			RowBuffer buffer = new RowBuffer(callbackSession, fetchSize, callback);
			T prev = null;
			while (true) {
				try {
					if (!cursor.next()) {
						break;
					}
				} catch (ObjectNotFoundException e) {
					continue;
				}
				T item = (T) cursor.get(0);
				if (prev != null && item != prev) {
					buffer.put(prev);
				}
				prev = item;
				if (buffer.shouldFlush()) {
					// put also the item/prev since we are clearing the
					// session
					// in the flush process
					buffer.put(prev);
					count += buffer.flush();
					prev = null;
					if (commitPerFetch) {
						transaction.commit();
						transaction = callbackSession.beginTransaction();
					}
				}
			}
			if (prev != null) {
				buffer.put(prev);
			}
			count += buffer.close();
			cursor.close();
			transaction.commit();
			return count;
		} catch (RuntimeException e) {
			logger.error(e.getMessage(), e);
			if (transaction != null && transaction.getStatus() == TransactionStatus.ACTIVE) {
				try {
					transaction.rollback();
				} catch (Exception e1) {
					logger.warn("Failed to rollback Hibernate", e1);
				}
			}
			throw e;
		} finally {
			try {
				if (callbackSession != iterateSession)
					callbackSession.close();
			} finally {
				iterateSession.close();
			}
		}
	}

	private class RowBuffer {
		private T[] buffer;
		private int currentIndex;
		private Session hibernateSession;
		private IterateCallback callback;

		RowBuffer(Session hibernateSession, int fetchSize, IterateCallback callback) {
			this.hibernateSession = hibernateSession;
			this.buffer = (T[]) Array.newInstance(getEntityClass(), fetchSize);
			this.callback = callback;
		}

		public void put(T row) {
			buffer[currentIndex] = row;
			currentIndex++;
		}

		public boolean shouldFlush() {
			return currentIndex >= buffer.length - 1;
		}

		public int close() {
			int i = flush();
			buffer = null;
			return i;
		}

		private int flush() {
			if (currentIndex == 0)
				return -1;
			callback.process(Arrays.copyOfRange(buffer, 0, currentIndex), hibernateSession);
			Arrays.fill(buffer, null);
			hibernateSession.flush();
			hibernateSession.clear();
			int result = currentIndex;
			currentIndex = 0;
			return result;
		}
	}

}
