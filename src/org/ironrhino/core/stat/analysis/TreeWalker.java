package org.ironrhino.core.stat.analysis;

public class TreeWalker {

	static interface Visitor {
		void visit(TreeNode node);
	}

	public static void walk(TreeNode node) {

	}

	public static void walk(TreeNode node, Visitor visitor) {
		walk(node, visitor, false);
	}

	public static void walk(TreeNode node, Visitor visitor, boolean postfix) {
		if (postfix) {
			for (TreeNode tn : node.getChildren())
				walk(tn, visitor, postfix);
			visitor.visit(node);
		} else {
			visitor.visit(node);
			for (TreeNode tn : node.getChildren())
				walk(tn, visitor, postfix);
		}
	}

}
