package org.ironrhino.core.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.Table;

import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.Query;
import org.hibernate.type.StringType;
import org.ironrhino.core.event.EntityOperationEvent;
import org.ironrhino.core.event.EntityOperationType;
import org.ironrhino.core.model.BaseTreeableEntity;
import org.ironrhino.core.throttle.Mutex;
import org.ironrhino.core.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.GenericTypeResolver;
import org.springframework.transaction.annotation.Transactional;

public abstract class BaseTreeControl<T extends BaseTreeableEntity<T>> {

	private volatile T tree;

	private final Class<T> entityClass;

	@Autowired
	private EntityManager<T> entityManager;

	@SuppressWarnings("unchecked")
	public BaseTreeControl() {
		entityClass = (Class<T>) GenericTypeResolver.resolveTypeArgument(getClass(), BaseTreeControl.class);
	}

	protected T buildTree() {
		entityManager.setEntityClass(entityClass);
		return entityManager.loadTree();
	}

	public synchronized void rebuild() {
		tree = null;
		getTree();
	}

	public T getTree() {
		T temp = tree;
		if (temp == null) {
			synchronized (this) {
				if ((temp = tree) == null)
					tree = temp = buildTree();
			}
		}
		return temp;
	}

	public T getTree(String name) {
		T subtree = null;
		for (T t : getTree().getChildren())
			if (t.getName().equals(name)) {
				addLevel(t, 1);
				subtree = t;
				break;
			}
		return subtree;
	}

	private void addLevel(T treeNode, int delta) {
		treeNode.setLevel(treeNode.getLevel() + delta);
		for (T t : treeNode.getChildren())
			addLevel(t, delta);
	}

	@Transactional
	@Mutex
	public void repairHierarchy() {
		entityManager.execute(session -> {
			String tableName = entityClass.getSimpleName();
			if (entityClass.isAnnotationPresent(Table.class))
				tableName = entityClass.getAnnotation(Table.class).name();
			SessionFactoryImplementor sf = ((SessionFactoryImplementor) session.getSessionFactory());
			Dialect dialect = sf.getServiceRegistry().getService(JdbcServices.class).getDialect();
			SQLFunction concat = dialect.getFunctions().get("concat");
			String levelColumn = dialect.quote(sf.getServiceRegistry().getService(JdbcEnvironment.class)
					.getIdentifierHelper().isReservedWord("level") ? "`level`" : "level");
			String fullId = concat.render(StringType.INSTANCE, Arrays.asList("id", "'.'"), sf);
			if (dialect instanceof SQLServerDialect || dialect instanceof SybaseDialect)
				fullId = "(convert(varchar,id)+'.')";
			Query<?> query = session.createNativeQuery(
					"update " + tableName + " set fullId=" + fullId + "," + levelColumn + "=1 where parentId is null");
			if (query.executeUpdate() > 0) {
				fullId = concat.render(StringType.INSTANCE, Arrays.asList("b.fullId", "a.id", "'.'"), sf);
				String sql;
				if (dialect instanceof MySQLDialect) {
					sql = "update " + tableName + " t join (select a.id," + fullId + " as fullId,b." + levelColumn
							+ "+1 as " + levelColumn + " from " + tableName + " a join " + tableName
							+ " b on a.parentId=b.id where b." + levelColumn
							+ "=:level) c on t.id=c.id set t.fullId=c.fullId,t." + levelColumn + "=c." + levelColumn;
				} else if (dialect instanceof PostgreSQL81Dialect || dialect instanceof DB2Dialect) {
					sql = "update " + tableName + " t set fullId=c.fullId," + levelColumn + "=c." + levelColumn
							+ " from (select a.id," + fullId + " as fullId,b." + levelColumn + "+1 as " + levelColumn
							+ " from " + tableName + " a join " + tableName + " b on a.parentId=b.id where b."
							+ levelColumn + "=:level) c where t.id=c.id";
				} else if (dialect instanceof Oracle8iDialect) {
					sql = "update (select a.fullId,a." + levelColumn + "," + fullId + " as newFullId,b." + levelColumn
							+ "+1 as newLevel from " + tableName + " a join " + tableName
							+ " b on a.parentId=b.id where b." + levelColumn + "=:level) t set t.fullId=t.newFullId,t."
							+ levelColumn + "=t.newLevel";
				} else if (dialect instanceof SQLServerDialect || dialect instanceof SybaseDialect) {
					sql = "update a set fullId=(b.fullId+convert(varchar,a.id)+'.')," + levelColumn + "=b."
							+ levelColumn + "+1 from " + tableName + " a join " + tableName
							+ " b on a.parentId=b.id where b." + levelColumn + "=:level";
				} else {
					sql = "update " + tableName + " a set (fullId," + levelColumn + ")=(select " + fullId + ",b."
							+ levelColumn + "+1 from " + tableName + " b where b.id=a.parentId and b." + levelColumn
							+ "=:level) where exists (select * from " + tableName + " d where d.id=a.parentId and d."
							+ levelColumn + "=:level)";
				}
				query = session.createNativeQuery(sql);
				int level = 1;
				while (true) {
					query.setParameter("level", level);
					if (query.executeUpdate() == 0)
						break;
					level++;
				}
			}
			return null;
		});
		tree = null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private synchronized void create(T treeNode) {
		T parent;
		String fullId = treeNode.getFullId();
		if (fullId.endsWith("."))
			fullId = fullId.substring(0, fullId.length() - 1);
		if (treeNode.getId().toString().equals(fullId)) {
			parent = tree;
		} else {
			String parentId = fullId.substring(0, fullId.lastIndexOf('.'));
			if (parentId.indexOf('.') > -1)
				parentId = parentId.substring(parentId.lastIndexOf('.') + 1);
			parent = tree.getDescendantOrSelfById(Long.valueOf(parentId));
		}
		try {
			T t = org.springframework.beans.BeanUtils.instantiateClass(entityClass);
			t.setChildren(new ArrayList<>());
			BeanUtils.copyProperties(treeNode, t, new String[] { "parent", "children" });
			parent.addChild(t);
			if (parent.getChildren() instanceof List)
				((List) parent.getChildren()).sort(null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private synchronized void update(T treeNode) {
		T t = tree.getDescendantOrSelfById(treeNode.getId());
		if (t == null)
			return;
		boolean needsort = t.compareTo(treeNode) != 0 || !t.getFullId().equals(treeNode.getFullId());
		if (!t.getFullId().equals(treeNode.getFullId())) {
			t.getParent().getChildren().remove(t);
			String str = treeNode.getFullId();
			if (str.endsWith("."))
				str = str.substring(0, str.length() - 1);
			long newParentId = 0;
			if (str.indexOf('.') > 0) {
				str = str.substring(0, str.lastIndexOf('.'));
				if (str.indexOf('.') > 0)
					str = str.substring(str.lastIndexOf('.') + 1);
				newParentId = Long.valueOf(str);
			}
			T newParent;
			if (newParentId == 0)
				newParent = tree;
			else
				newParent = tree.getDescendantOrSelfById(newParentId);
			t.setParent(newParent);
			newParent.getChildren().add(t);
			resetChildren(t);
		}
		BeanUtils.copyProperties(treeNode, t, new String[] { "parent", "children" });
		if (needsort && t.getParent().getChildren() instanceof List)
			((List) t.getParent().getChildren()).sort(null);
	}

	private void resetChildren(T treeNode) {
		if (treeNode.isHasChildren())
			for (T t : treeNode.getChildren()) {
				String fullId = (t.getParent()).getFullId() + String.valueOf(t.getId()) + ".";
				t.setFullId(fullId);
				t.setLevel(fullId.split("\\.").length);
				treeNode.setParent(treeNode.getParent());
				resetChildren(t);
			}
	}

	private synchronized void delete(T treeNode) {
		T t = tree.getDescendantOrSelfById(treeNode.getId());
		if (t != null)
			t.getParent().getChildren().remove(t);
	}

	@EventListener
	public void onApplicationEvent(EntityOperationEvent<T> event) {
		if (tree == null)
			return;
		if (event.getEntity().getClass() == entityClass) {
			T treeNode = event.getEntity();
			if (event.getType() == EntityOperationType.CREATE)
				create(treeNode);
			else if (event.getType() == EntityOperationType.UPDATE)
				update(treeNode);
			else if (event.getType() == EntityOperationType.DELETE)
				delete(treeNode);
		}
	}
}
