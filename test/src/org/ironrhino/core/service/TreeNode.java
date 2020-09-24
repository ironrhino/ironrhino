package org.ironrhino.core.service;

import javax.persistence.Entity;
import javax.persistence.Table;

import org.ironrhino.core.model.BaseTreeableEntity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "test_treenode")
@Getter
@Setter
@NoArgsConstructor
public class TreeNode extends BaseTreeableEntity<TreeNode> {

	private static final long serialVersionUID = 0L;

	public TreeNode(String name) {
		this.name = name;
	}

	public TreeNode(String name, int displayOrder) {
		this.name = name;
		this.displayOrder = displayOrder;
	}

}
