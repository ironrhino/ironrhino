package org.ironrhino.common.action;

import java.util.ArrayList;
import java.util.Collection;

import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.ironrhino.common.model.Region;
import org.ironrhino.core.hibernate.CriterionUtils;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.JsonConfig;
import org.ironrhino.core.service.BaseTreeControl;
import org.ironrhino.core.service.EntityManager;
import org.ironrhino.core.struts.BaseAction;
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
	private EntityManager<Region> entityManager;

	@Autowired(required = false)
	private BaseTreeControl<Region> regionTreeControl;

	@JsonConfig(root = "children")
	public String children() {
		entityManager.setEntityClass(Region.class);
		Region region;
		if (parent == null || parent < 1) {
			if (tree != null && tree > 0) {
				children = new ArrayList<>();
				if (regionTreeControl != null) {
					children.add(regionTreeControl.getTree().getDescendantOrSelfById(tree));
				} else {
					region = new Region();
					DetachedCriteria dc = entityManager.detachedCriteria();
					dc.add(Restrictions.eq("id", tree));
					region.setChildren(entityManager.findListByCriteria(dc));
				}
				return JSON;
			} else {
				if (regionTreeControl != null) {
					region = regionTreeControl.getTree();
				} else {
					region = new Region();
					DetachedCriteria dc = entityManager.detachedCriteria();
					dc.add(Restrictions.isNull("parent"));
					dc.addOrder(Order.asc("displayOrder"));
					dc.addOrder(Order.asc("name"));
					region.setChildren(entityManager.findListByCriteria(dc));
				}
			}
		} else {
			if (regionTreeControl != null) {
				region = regionTreeControl.getTree().getDescendantOrSelfById(parent);
			} else {
				region = entityManager.get(parent);
			}
		}
		if (region != null)
			children = region.getChildren();
		children = CriterionUtils.filter(children);
		return JSON;
	}

	public String table() {
		return "table";
	}

	public Region getRegionTree() {
		entityManager.setEntityClass(Region.class);
		Region region;
		if (regionTreeControl != null) {
			region = regionTreeControl.getTree();
		} else {
			region = new Region();
			DetachedCriteria dc = entityManager.detachedCriteria();
			dc.add(Restrictions.isNull("parent"));
			dc.addOrder(Order.asc("displayOrder"));
			dc.addOrder(Order.asc("name"));
			region.setChildren(entityManager.findListByCriteria(dc));
		}
		return region;
	}

}
