package org.ironrhino.common.action;

import java.util.ArrayList;
import java.util.Collection;

import org.ironrhino.common.model.Region;
import org.ironrhino.core.hibernate.CriterionUtils;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.JsonConfig;
import org.ironrhino.core.service.BaseTreeControl;
import org.ironrhino.core.struts.BaseAction;
import org.ironrhino.core.util.HtmlUtils;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.Getter;
import lombok.Setter;

@AutoConfig(namespace = "/", actionName = "region")
public class RegionTreeAction extends BaseAction {

	private static final long serialVersionUID = -1333891551369466096L;

	@Getter
	private Collection<Region> children;

	@Getter
	@Setter
	private boolean async = true;

	@Getter
	@Setter
	private Long tree;

	@Getter
	@Setter
	private Long parent;

	@Autowired
	private BaseTreeControl<Region> regionTreeControl;

	@JsonConfig(root = "children")
	public String children() {
		Region region;
		if (parent == null || parent < 1) {
			if (tree != null && tree > 0) {
				children = new ArrayList<>();
				children.add(regionTreeControl.getTree().getDescendantOrSelfById(tree));
				return JSON;
			} else {
				region = regionTreeControl.getTree();
			}
		} else {
			region = regionTreeControl.getTree().getDescendantOrSelfById(parent);
		}
		if (region != null)
			children = region.getChildren();
		children = CriterionUtils.filter(children);
		return JSON;
	}

	@Override
	public String execute() {
		if (!async) {
			Region region;
			if (parent == null || parent < 1)
				region = regionTreeControl.getTree();
			else
				region = regionTreeControl.getTree().getDescendantOrSelfById(parent);
			children = region.getChildren();
		}
		children = CriterionUtils.filter(children);
		return SUCCESS;
	}

	public String table() {
		return "table";
	}

	public Region getRegionTree() {
		return regionTreeControl.getTree();
	}

	public String getTreeViewHtml() {
		return HtmlUtils.getTreeViewHtml(children, async);
	}

}
