package org.ironrhino.core.stat.analysis;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.stat.Key;
import org.ironrhino.core.stat.Value;
import org.ironrhino.core.util.NumberUtils;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TreeNode implements Serializable {

	private static final long serialVersionUID = 5312284581467948055L;

	private int id;

	// make transient for json serialization
	private transient TreeNode parent;

	private List<TreeNode> children = new ArrayList<>();

	private Key key;

	private Value value;

	private String longPercent;

	private String doublePercent;

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

	public TreeNode getDescendantOrSelfByKey(Key key) {
		if (key == null)
			throw new IllegalArgumentException("key must not be null");
		if (key.equals(this.getKey()))
			return this;
		for (TreeNode t : getChildren()) {
			if (key.equals(t.getKey())) {
				return t;
			} else {
				TreeNode tt = t.getDescendantOrSelfByKey(key);
				if (tt != null)
					return tt;
			}
		}
		return null;
	}

	public void filter(String filter) {
		Iterator<TreeNode> it = children.iterator();
		while (it.hasNext()) {
			TreeNode node = it.next();
			String path = StringUtils.join(node.getKey().getNames(), ">");
			if (filter.equals(path) || path.startsWith(filter + ">") || filter.startsWith(path + ">"))
				node.filter(filter);
			else
				it.remove();
		}
	}

	public void calculate() {
		TreeWalker.Visitor vistor = node -> {
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
					n.setLongPercent(NumberUtils
							.formatPercent(((double) n.getValue().getLongValue()) / node.getValue().getLongValue(), 2));
				if (n.getValue().getDoubleValue() > 0)
					n.setDoublePercent(NumberUtils
							.formatPercent(n.getValue().getDoubleValue() / node.getValue().getDoubleValue(), 2));
			}
		};
		TreeWalker.walk(this, vistor, true);
	}

}
