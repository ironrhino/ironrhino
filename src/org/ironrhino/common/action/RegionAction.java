package org.ironrhino.common.action;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.ironrhino.common.model.Region;
import org.ironrhino.core.hibernate.CriterionUtils;
import org.ironrhino.core.metadata.JsonConfig;
import org.ironrhino.core.model.LabelValue;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.search.SearchService.Mapper;
import org.ironrhino.core.search.elasticsearch.ElasticSearchCriteria;
import org.ironrhino.core.search.elasticsearch.ElasticSearchService;
import org.ironrhino.core.service.EntityManager;
import org.ironrhino.core.struts.BaseAction;
import org.ironrhino.core.util.ClassScaner;
import org.ironrhino.core.util.HtmlUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.validator.annotations.RequiredFieldValidator;
import com.opensymphony.xwork2.validator.annotations.RequiredStringValidator;
import com.opensymphony.xwork2.validator.annotations.StringLengthFieldValidator;
import com.opensymphony.xwork2.validator.annotations.Validations;
import com.opensymphony.xwork2.validator.annotations.ValidatorType;

@SuppressWarnings(value = { "rawtypes", "unchecked" })
public class RegionAction extends BaseAction {

	private static final long serialVersionUID = -4643055307938016102L;

	private Region region;

	private Long parent;

	private transient EntityManager<Region> entityManager;

	private Collection list;

	private String southWest;

	private String northEast;

	private int zoom;

	private boolean async;

	@Autowired(required = false)
	private transient ElasticSearchService<Region> elasticSearchService;

	public boolean isAsync() {
		return async;
	}

	public void setAsync(boolean async) {
		this.async = async;
	}

	public int getZoom() {
		return zoom;
	}

	public void setZoom(int zoom) {
		this.zoom = zoom;
	}

	public void setSouthWest(String southWest) {
		this.southWest = southWest;
	}

	public void setNorthEast(String northEast) {
		this.northEast = northEast;
	}

	public Collection getList() {
		return list;
	}

	public Long getParent() {
		return parent;
	}

	public void setParent(Long parent) {
		this.parent = parent;
	}

	public Region getRegion() {
		return region;
	}

	public void setRegion(Region region) {
		this.region = region;
	}

	public void setEntityManager(EntityManager<Region> entityManager) {
		entityManager.setEntityClass(Region.class);
		this.entityManager = entityManager;
	}

	@Override
	public String execute() {
		if (StringUtils.isBlank(keyword) || elasticSearchService == null) {
			if (parent != null && parent > 0) {
				region = entityManager.get(parent);
			} else {
				region = new Region();
				DetachedCriteria dc = entityManager.detachedCriteria();
				dc.add(Restrictions.isNull("parent"));
				dc.addOrder(Order.asc("displayOrder"));
				dc.addOrder(Order.asc("name"));
				if (StringUtils.isNotBlank(keyword))
					dc.add(CriterionUtils.like(keyword, MatchMode.ANYWHERE,
							"name", "areacode", "postcode"));
				region.setChildren(entityManager.findListByCriteria(dc));
			}
			list = region.getChildren();
		} else {
			String query = keyword.trim();
			ElasticSearchCriteria criteria = new ElasticSearchCriteria();
			criteria.setQuery(query);
			criteria.setTypes(new String[] { "region" });
			criteria.addSort("displayOrder", false);
			list = elasticSearchService.search(criteria, new Mapper<Region>() {
				@Override
				public Region map(Region source) {
					return entityManager.get(source.getId());
				}
			});
		}
		return LIST;
	}

	@Override
	public String input() {
		if (getUid() != null)
			region = entityManager.get(Long.valueOf(getUid()));
		if (region == null)
			region = new Region();
		return INPUT;
	}

	@Override
	@Validations(requiredStrings = { @RequiredStringValidator(type = ValidatorType.FIELD, fieldName = "region.name", trim = true, key = "validation.required") }, stringLengthFields = {
			@StringLengthFieldValidator(type = ValidatorType.FIELD, fieldName = "region.areacode", maxLength = "6", key = "validation.invalid"),
			@StringLengthFieldValidator(type = ValidatorType.FIELD, fieldName = "region.postcode", maxLength = "6", key = "validation.invalid") })
	public String save() {
		Collection<Region> siblings = null;
		if (region.isNew()) {
			if (parent != null) {
				Region parentRegion = entityManager.get(parent);
				region.setParent(parentRegion);
				siblings = parentRegion.getChildren();
			} else {
				DetachedCriteria dc = entityManager.detachedCriteria();
				dc.add(Restrictions.isNull("parent"));
				dc.addOrder(Order.asc("displayOrder"));
				dc.addOrder(Order.asc("name"));
				siblings = entityManager.findListByCriteria(dc);
			}
			for (Region sibling : siblings)
				if (sibling.getName().equals(region.getName())) {
					addFieldError("region.name",
							getText("validation.already.exists"));
					return INPUT;
				}
		} else {
			Region temp = region;
			region = entityManager.get(temp.getId());
			if (ServletActionContext.getRequest().getParameter(
					"region.coordinate.latLngAsString") != null) {
				region.setCoordinate(temp.getCoordinate());
			}
			if (!region.getName().equals(temp.getName())) {
				if (region.getParent() == null) {
					DetachedCriteria dc = entityManager.detachedCriteria();
					dc.add(Restrictions.isNull("parent"));
					dc.addOrder(Order.asc("displayOrder"));
					dc.addOrder(Order.asc("name"));
					siblings = entityManager.findListByCriteria(dc);
				} else {
					siblings = region.getParent().getChildren();
				}
				for (Region sibling : siblings)
					if (sibling.getName().equals(temp.getName())) {
						addFieldError("region.name",
								getText("validation.already.exists"));
						return INPUT;
					}
			}
			region.setName(temp.getName());
			region.setAreacode(temp.getAreacode());
			region.setPostcode(temp.getPostcode());
			region.setRank(temp.getRank());
			region.setDisplayOrder(temp.getDisplayOrder());
		}

		entityManager.save(region);
		addActionMessage(getText("save.success"));
		return SUCCESS;
	}

	@Override
	public String delete() {
		String[] id = getId();
		if (id != null) {
			entityManager.setEntityClass(Region.class);
			entityManager.delete((Serializable[]) id);
			addActionMessage(getText("delete.success"));
		}
		return SUCCESS;
	}

	public String getTreeViewHtml() {
		return HtmlUtils.getTreeViewHtml(list, async);
	}

	public String map() {
		return "map";
	}

	@Validations(requiredFields = {
			@RequiredFieldValidator(type = ValidatorType.FIELD, fieldName = "region.id", key = "validation.required"),
			@RequiredFieldValidator(type = ValidatorType.FIELD, fieldName = "region.coordinate.latitude", key = "validation.required"),
			@RequiredFieldValidator(type = ValidatorType.FIELD, fieldName = "region.coordinate.longitude", key = "validation.required") })
	public String mark() {
		Region temp = region;
		region = entityManager.get(region.getId());
		region.setCoordinate(temp.getCoordinate());
		entityManager.save(region);
		addActionMessage(getText("save.success"));
		return JSON;
	}

	@JsonConfig(root = "list")
	public String markers() {
		String[] array = southWest.split(",");
		Double bottom = new Double(array[0]);
		Double left = new Double(array[1]);
		array = northEast.split(",");
		Double top = new Double(array[0]);
		Double right = new Double(array[1]);
		Integer[] levels = zoom2level(zoom);
		Integer[] ranks = zoom2rank(zoom);
		DetachedCriteria dc = entityManager.detachedCriteria();
		if (levels != null && ranks != null)
			dc.add(Restrictions.or(Restrictions.in("level", levels),
					Restrictions.in("rank", ranks)));
		else if (levels != null)
			dc.add(Restrictions.in("level", levels));
		else if (ranks != null)
			dc.add(Restrictions.in("rank", ranks));
		dc.add(Restrictions.and(
				Restrictions.between("coordinate.latitude", bottom, top),
				Restrictions.between("coordinate.longitude", left, right)));
		list = entityManager.findListByCriteria(dc);
		return JSON;
	}

	private Integer[] zoom2level(int z) {
		if (z <= 5) {
			return new Integer[] { 1 };
		} else if (z <= 8) {
			return new Integer[] { 1, 2 };
		} else if (z <= 9) {
			return new Integer[] { 1, 2, 3 };
		} else {
			return null;
		}
	}

	private Integer[] zoom2rank(int z) {
		if (z <= 5) {
			return new Integer[] { 1, 2 };
		} else {
			return null;
		}
	}

	public String move() {
		String[] id = getId();
		if (id != null && id.length == 2) {
			Region source = null;
			Region target = null;
			try {
				source = entityManager.get(Long.valueOf(id[0]));
				if (Long.valueOf(id[1]) > 0)
					target = entityManager.get(Long.valueOf(id[1]));
			} catch (Exception e) {

			}
			if (source == null) {
				addActionError(getText("validation.required"));
				return SUCCESS;
			}
			if (target != null && source.getId().equals(target.getId())) {
				addActionError(getText("validation.invalid"));
				return SUCCESS;
			}
			if (!(source.getParent() == null && target == null || source
					.getParent() != null
					&& target != null
					&& source.getParent().getId().equals(target.getId()))) {
				source.setParent(target);
				entityManager.save(source);
				addActionMessage(getText("operate.success"));
			}
		}
		return SUCCESS;
	}

	public String merge() {
		String[] id = getId();
		if (id != null && id.length == 2) {
			Region source = null;
			Region target = null;
			try {
				source = entityManager.get(Long.valueOf(id[0]));
				target = entityManager.get(Long.valueOf(id[1]));
			} catch (Exception e) {

			}
			if (source == null || target == null) {
				addActionError(getText("validation.required"));
				return SUCCESS;
			}
			if (!source.isLeaf() || !target.isLeaf()
					|| source.getId().equals(target.getId())) {
				addActionError(getText("validation.invalid"));
				return SUCCESS;
			}
			Set<Class<?>> set = ClassScaner.scanAssignable(
					ClassScaner.getAppPackages(), Persistable.class);
			for (Class<?> clz : set) {
				if (clz.equals(Region.class))
					continue;
				PropertyDescriptor[] pds = BeanUtils
						.getPropertyDescriptors(clz);
				for (PropertyDescriptor pd : pds) {
					if (pd.getReadMethod() != null
							&& pd.getReadMethod().getReturnType()
									.equals(Region.class)
							&& pd.getWriteMethod() != null) {
						String name = pd.getName();
						String hql = new StringBuilder("update ")
								.append(clz.getName()).append(" t set t.")
								.append(name).append(".id=?1 where t.")
								.append(name).append(".id=?2").toString();
						entityManager.executeUpdate(hql, target.getId(),
								source.getId());
					}
				}
			}
			entityManager.delete(source);
			addActionMessage(getText("operate.success"));
		}
		return SUCCESS;
	}

	@JsonConfig(root = "list")
	public String unmarked() {
		DetachedCriteria dc = entityManager.detachedCriteria();
		dc.add(Restrictions.isNull("coordinate.latitude"));
		String uid = getUid();
		if (StringUtils.isNotBlank(uid)) {
			dc.add(Restrictions.lt("id", Long.valueOf(uid)));
		}
		dc.addOrder(Order.asc("id"));
		List<Region> result = entityManager.findListByCriteria(dc);
		list = new ArrayList(result.size());
		for (Region r : result)
			list.add(new LabelValue(r.getFullname(), r.getId().toString()));
		return JSON;
	}
}
