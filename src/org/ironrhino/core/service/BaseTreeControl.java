package org.ironrhino.core.service;

import java.util.ArrayList;
import java.util.List;

import org.ironrhino.core.event.EntityOperationEvent;
import org.ironrhino.core.event.EntityOperationType;
import org.ironrhino.core.model.BaseTreeableEntity;
import org.ironrhino.core.util.BeanUtils;
import org.ironrhino.core.util.ReflectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;

public class BaseTreeControl<T extends BaseTreeableEntity<T>> {

	private volatile T tree;

	private Class<T> entityClass;

	@Autowired
	private EntityManager<T> entityManager;

	public BaseTreeControl() {
		@SuppressWarnings("unchecked")
		Class<T> clazz = (Class<T>) ReflectionUtils.getGenericClass(getClass(), BaseTreeControl.class);
		if (clazz == null)
			throw new RuntimeException("generic class is required here");
		entityClass = clazz;
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
				temp = tree;
				if (temp == null) {
					temp = buildTree();
					tree = temp;
				}
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
			T t = entityClass.getConstructor().newInstance();
			t.setChildren(new ArrayList<>());
			BeanUtils.copyProperties(treeNode, t, new String[] { "parent", "children" });
			t.setParent(parent);
			parent.getChildren().add(t);
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
