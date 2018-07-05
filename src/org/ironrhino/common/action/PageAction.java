package org.ironrhino.common.action;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.ironrhino.common.model.Page;
import org.ironrhino.common.service.PageManager;
import org.ironrhino.core.hibernate.CriteriaState;
import org.ironrhino.core.hibernate.CriterionUtils;
import org.ironrhino.core.metadata.JsonConfig;
import org.ironrhino.core.model.LabelValue;
import org.ironrhino.core.model.ResultPage;
import org.ironrhino.core.search.SearchCriteria;
import org.ironrhino.core.struts.EntityAction;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.validator.annotations.RequiredStringValidator;
import com.opensymphony.xwork2.validator.annotations.Validations;
import com.opensymphony.xwork2.validator.annotations.ValidatorType;

import lombok.Getter;
import lombok.Setter;

public class PageAction extends EntityAction<String, Page> {

	private static final long serialVersionUID = 67252386921293136L;

	@Getter
	@Setter
	private Page page;

	@Getter
	private boolean draft;

	@Autowired
	private PageManager pageManager;

	private String cmsPath = "/p/";

	@com.opensymphony.xwork2.inject.Inject(value = "ironrhino.cmsPath", required = false)
	public void setCmsPath(String val) {
		cmsPath = val;
	}

	public String getCmsPath() {
		if (cmsPath.endsWith("/"))
			return cmsPath.substring(0, cmsPath.length() - 1);
		return cmsPath;
	}

	@Override
	public String execute() {
		if (StringUtils.isBlank(keyword) || searchService == null) {
			DetachedCriteria dc = pageManager.detachedCriteria();
			CriteriaState criteriaState = CriterionUtils.filter(dc, getEntityClass());
			if (StringUtils.isNotBlank(keyword)) {
				if (keyword.startsWith("tags:")) {
					String tags = keyword.replace("tags:", "");
					tags = tags.replace(" AND ", ",");
					for (String tag : tags.split("\\s*,\\s*"))
						dc.add(CriterionUtils.matchTag("tags", tag));
				} else {
					dc.add(CriterionUtils.like(keyword, "path", "title"));
				}
			}
			if (criteriaState.getOrderings().isEmpty()) {
				dc.addOrder(Order.asc("displayOrder"));
				dc.addOrder(Order.asc("path"));
			}
			if (resultPage == null)
				resultPage = new ResultPage<>();
			resultPage.setCriteria(dc);
			resultPage = pageManager.findByResultPage(resultPage);
		} else {
			String query = keyword.trim();
			String url = ServletActionContext.getRequest().getRequestURL().toString();
			String referer = ServletActionContext.getRequest().getHeader("Referer");
			if (referer != null && referer.startsWith(url))
				try {
					Thread.sleep(1000); // wait index
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			SearchCriteria criteria = new SearchCriteria();
			criteria.setQuery(query);
			criteria.setTypes(new String[] { "page" });
			criteria.addSort("displayOrder", false);
			criteria.addSort("path", false);
			if (resultPage == null)
				resultPage = new ResultPage<>();
			resultPage.setCriteria(criteria);
			resultPage = searchService.search(resultPage);
		}
		return LIST;
	}

	@Override
	public String input() {
		String id = getUid();
		if (StringUtils.isNotBlank(id)) {
			page = pageManager.get(id);
			if (page == null)
				page = pageManager.findByNaturalId(id);
			if (page == null && !id.startsWith("/"))
				page = pageManager.findByNaturalId("/" + id);
		} else if (page != null) {
			if (page.getId() != null)
				page = pageManager.get(page.getId());
			else if (page.getPath() != null)
				page = pageManager.findByNaturalId(page.getPath());
		}
		if (page == null) {
			page = new Page();
			if (StringUtils.isNotBlank(id))
				page.setPath(id.startsWith("/") ? id : "/" + id);
			if (StringUtils.isNotBlank(keyword) && keyword.startsWith("tags:")) {
				String tags = keyword.replace("tags:", "");
				tags = tags.replace(" AND ", ",");
				String[] tagsArray = tags.split("\\s*,\\s*");
				String tag = tagsArray[0];
				int count = pageManager.findListByTag(tag).size();
				String path = null;
				while (true) {
					path = '/' + tag + '/' + (++count);
					if (pageManager.getByPath(path) == null)
						break;
				}
				page.setPath(path);
				page.setTags(new HashSet<>(Arrays.asList(tagsArray)));
			}
		} else {
			if (StringUtils.isNotBlank(page.getDraft())) {
				draft = true;
				pageManager.pullDraft(page);
			}
		}
		return INPUT;
	}

	@Override
	@JsonConfig(propertyName = "page")
	@Validations(requiredStrings = {
			@RequiredStringValidator(type = ValidatorType.FIELD, fieldName = "page.path", trim = true, key = "validation.required") })
	public String save() {
		if (!makeEntityValid())
			return INPUT;
		if (!page.isNew()) {
			Page temp = page;
			page = pageManager.get(page.getId());
			if (temp.getVersion() > -1 && temp.getVersion() != page.getVersion()) {
				addActionError(getText("validation.version.conflict"));
				return INPUT;
			}
			page.setTags(temp.getTags());
			page.setDisplayOrder(temp.getDisplayOrder());
			page.setTitle(temp.getTitle());
			page.setHead(temp.getHead());
			page.setContent(temp.getContent());
		}
		pageManager.save(page);
		notify("save.success");
		return JSON;
	}

	@JsonConfig(propertyName = "page")
	@Validations(requiredStrings = {
			@RequiredStringValidator(type = ValidatorType.FIELD, fieldName = "page.path", trim = true, key = "validation.required"),
			@RequiredStringValidator(type = ValidatorType.FIELD, fieldName = "page.content", trim = true, key = "validation.required") })
	public String draft() {
		if (!makeEntityValid())
			return INPUT;
		page = pageManager.saveDraft(page);
		pageManager.pullDraft(page);
		draft = true;
		return INPUT;
	}

	@Override
	@Validations(requiredStrings = {
			@RequiredStringValidator(type = ValidatorType.FIELD, fieldName = "page.path", trim = true, key = "validation.required") })
	public String checkavailable() {
		makeEntityValid();
		return JSON;
	}

	@Override
	protected boolean makeEntityValid() {
		String path = page.getPath().trim();
		if (!path.startsWith("/"))
			path = '/' + path;
		page.setPath(path);
		if (page.isNew()) {
			if (pageManager.findByNaturalId(page.getPath()) != null) {
				addFieldError("page.path", getText("validation.already.exists"));
				return false;
			}
		} else {
			Page p = pageManager.get(page.getId());
			if (!page.getPath().equals(p.getPath()) && pageManager.findByNaturalId(page.getPath()) != null) {
				addFieldError("page.path", getText("validation.already.exists"));
				return false;
			}
		}
		return true;
	}

	public String drop() {
		page = pageManager.dropDraft(page.getId());
		return INPUT;
	}

	@Override
	public String delete() {
		String[] id = getId();
		if (id != null) {
			pageManager.delete((Serializable[]) id);
			notify("delete.success");
		}
		return SUCCESS;
	}

	@Validations(requiredStrings = {
			@RequiredStringValidator(type = ValidatorType.FIELD, fieldName = "page.content", trim = true, key = "validation.required") })
	public String editme() {
		String content = page.getContent();
		page = pageManager.get(getUid());
		page.setContent(content);
		pageManager.save(page);
		notify("save.success");
		return JSON;
	}

	@SuppressWarnings("unchecked")
	@JsonConfig(root = "suggestions")
	public String suggest() {
		if (StringUtils.isBlank(keyword))
			return NONE;
		Map<String, Integer> map = pageManager.findMatchedTags(keyword);
		suggestions = new ArrayList<>(map.size());
		for (Map.Entry<String, Integer> entry : map.entrySet()) {
			LabelValue lv = new LabelValue();
			lv.setValue(entry.getKey());
			lv.setLabel(new StringBuilder(entry.getKey()).append("(").append(entry.getValue()).append(")").toString());
			suggestions.add(lv);
		}
		return JSON;
	}

}