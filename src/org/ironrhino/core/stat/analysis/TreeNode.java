package org.ironrhino.core.stat.analysis;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.stat.Key;
import org.ironrhino.core.stat.Value;
import org.ironrhino.core.util.NumberUtils;

public class TreeNode implements Serializable {

	private static final long serialVersionUID = 5312284581467948055L;

	private int id;

	// make transient for json serialization
	private transient TreeNode parent;

	private List<TreeNode> children = new ArrayList<TreeNode>();

	private Key key;

	private Value value;

	private String longPercent;

	private String doublePercent;

	public String getLongPercent() {
		return longPercent;
	}

	public void setLongPercent(String longPercent) {
		this.longPercent = longPercent;
	}

	public String getDoublePercent() {
		return doublePercent;
	}

	public void setDoublePercent(String doublePercent) {
		this.doublePercent = doublePercent;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Key getKey() {
		return key;
	}

	public void setKey(Key key) {
		this.key = key;
	}

	public TreeNode getParent() {
		return parent;
	}

	public void setParent(TreeNode parent) {
		this.parent = parent;
	}

	public List<TreeNode> getChildren() {
		return children;
	}

	public void setChildren(List<TreeNode> children) {
		this.children = children;
	}

	public Value getValue() {
		return value;
	}

	public void setValue(Value value) {
		this.value = value;
	}

	public boolean isRoot() {
		return parent == null;
	}

	public boolean isLeaf() {
		return children == null || children.size() == 0;
	}

	public int getLevel() {
		int level = 1;
		TreeNode node = this;
		while ((node = node.getParent()) != null)
			level++;
		return level;
	}

	public String getName() {
		return key.getNames()[getLevel() - 1];
	}

	public void filter(String filter) {
		Iterator<TreeNode> it = children.iterator();
		while (it.hasNext()) {
			TreeNode node = it.next();
			String path = StringUtils.join(node.getKey().getNames(), ">");
			if (filter.equals(path) || path.startsWith(filter + ">")
					|| filter.startsWith(path + ">"))
				node.filter(filter);
			else
				it.remove();
		}
	}

	public void calculate() {
		TreeWalker.Visitor vistor = new TreeWalker.Visitor() {
			@Override
			public void visit(TreeNode node) {
				if (node.isLeaf())
					return;
				long longValue = 0;
				double doubleValue = 0;
				for (TreeNode n : node.getChildren()) {
					longValue += n.getValue().getLongValue();
					doubleValue += n.getValue().getDoubleValue();
				}
				node.setValue(new Value(longValue, doubleValue));
				for (TreeNode n : node.getChildren()) {
					if (n.getValue().getLongValue() > 0)
						n.setLongPercent(NumberUtils.formatPercent(((double) n
								.getValue().getLongValue())
								/ node.getValue().getLongValue(), 2));
					if (n.getValue().getDoubleValue() > 0)
						n.setDoublePercent(NumberUtils.formatPercent(n
								.getValue().getDoubleValue()
								/ node.getValue().getDoubleValue(), 2));
				}
			}
		};
		TreeWalker.walk(this, vistor, true);
	}

}
