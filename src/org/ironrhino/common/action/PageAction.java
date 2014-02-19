package org.ironrhino.common.action;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.ironrhino.common.model.Page;
import org.ironrhino.common.service.PageManager;
import org.ironrhino.core.hibernate.CriteriaState;
import org.ironrhino.core.hibernate.CriterionUtils;
import org.ironrhino.core.metadata.JsonConfig;
import org.ironrhino.core.model.LabelValue;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.model.ResultPage;
import org.ironrhino.core.search.elasticsearch.ElasticSearchCriteria;
import org.ironrhino.core.search.elasticsearch.ElasticSearchService;
import org.ironrhino.core.struts.BaseAction;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.validator.annotations.RequiredStringValidator;
import com.opensymphony.xwork2.validator.annotations.Validations;
import com.opensymphony.xwork2.validator.annotations.ValidatorType;

public class PageAction extends BaseAction {

	private static final long serialVersionUID = 67252386921293136L;

	private Page page;

	private boolean draft;

	private ResultPage<Page> resultPage;

	@Autowired
	private transient PageManager pageManager;

	private String cmsPath = "/p/";

	@Autowired(required = false)
	private transient ElasticSearchService<Page> elasticSearchService;

	public Class<? extends Persistable<?>> getEntityClass() {
		return Page.class;
	}

	@com.opensymphony.xwork2.inject.Inject(value = "ironrhino.cmsPath", required = false)
	public void setCmsPath(String val) {
		cmsPath = val;
	}

	public boolean isDraft() {
		return draft;
	}

	public String getCmsPath() {
		if (cmsPath.endsWith("/"))
			return cmsPath.substring(0, cmsPath.length() - 1);
		return cmsPath;
	}

	public Page getPage() {
		return page;
	}

	public void setPage(Page page) {
		this.page = page;
	}

	public ResultPage<Page> getResultPage() {
		return resultPage;
	}

	public void setResultPage(ResultPage<Page> resultPage) {
		this.resultPage = resultPage;
	}

	@Override
	public String execute() {
		if (StringUtils.isBlank(keyword) || elasticSearchService == null) {
			DetachedCriteria dc = pageManager.detachedCriteria();
			CriteriaState criteriaState = CriterionUtils.filter(dc,
					getEntityClass());
			if (StringUtils.isNotBlank(keyword)) {
				if (keyword.startsWith("tags:")) {
					String tags = keyword.replace("tags:", "");
					tags = tags.replace(" AND ", ",");
					for (String tag : tags.split("\\s*,\\s*"))
						dc.add(CriterionUtils.matchTag("tagsAsString", tag));
				} else {
					dc.add(CriterionUtils.like(keyword, MatchMode.ANYWHERE,
							"path", "title"));
				}
			}
			if (criteriaState.getOrderings().isEmpty()) {
				dc.addOrder(Order.asc("displayOrder"));
				dc.addOrder(Order.asc("path"));
			}
			if (resultPage == null)
				resultPage = new ResultPage<Page>();
			resultPage.setCriteria(dc);
			resultPage = pageManager.findByResultPage(resultPage);
		} else {
			String query = keyword.trim();
			String url = ServletActionContext.getRequest().getRequestURL()
					.toString();
			String referer = ServletActionContext.getRequest().getHeader(
					"Referer");
			if (referer != null && referer.startsWith(url))
				try {
					Thread.sleep(1000); // wait index
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			ElasticSearchCriteria criteria = new ElasticSearchCriteria();
			criteria.setQuery(query);
			criteria.setTypes(new String[] { "page" });
			criteria.addSort("displayOrder", false);
			criteria.addSort("path", false);
			if (resultPage == null)
				resultPage = new ResultPage<Page>();
			resultPage.setCriteria(criteria);
			resultPage = elasticSearchService.search(resultPage);
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
				String tag = tags.split("\\s*,\\s*")[0];
				int count = pageManager.findListByTag(tag).size();
				String path = null;
				while (true) {
					path = "/" + tag + "/" + (++count);
					if (pageManager.getByPath(path) == null)
						break;
				}
				page.setPath(path);
				page.setTagsAsString(tags);
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
	@Validations(requiredStrings = { @RequiredStringValidator(type = ValidatorType.FIELD, fieldName = "page.path", trim = true, key = "validation.required") })
	public String save() {
		if (!makeEntityValid())
			return INPUT;
		if (!page.isNew()) {
			Page temp = page;
			page = pageManager.get(page.getId());
			if (temp.getVersion() > -1 && temp.getVersion() < page.getVersion()) {
				addActionError(getText("validation.version.conflict"));
				return INPUT;
			}
			page.setPath(temp.getPath());
			page.setTags(temp.getTags());
			page.setDisplayOrder(temp.getDisplayOrder());
			page.setTitle(temp.getTitle());
			page.setHead(temp.getHead());
			page.setContent(temp.getContent());
		}
		pageManager.save(page);
		addActionMessage(getText("save.success"));
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

	@Validations(requiredStrings = { @RequiredStringValidator(type = ValidatorType.FIELD, fieldName = "page.path", trim = true, key = "validation.required") })
	public String checkavailable() {
		return makeEntityValid() ? NONE : INPUT;
	}

	private boolean makeEntityValid() {
		String path = page.getPath().trim().toLowerCase();
		if (!path.startsWith("/"))
			path = "/" + path;
		page.setPath(path);
		if (page.isNew()) {
			if (pageManager.findByNaturalId(page.getPath()) != null) {
				addFieldError("page.path", getText("validation.already.exists"));
				return false;
			}
		} else {
			Page p = pageManager.get(page.getId());
			if (!page.getPath().equals(p.getPath())
					&& pageManager.findByNaturalId(page.getPath()) != null) {
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
			addActionMessage(getText("delete.success"));
		}
		return SUCCESS;
	}

	@Validations(requiredStrings = { @RequiredStringValidator(type = ValidatorType.FIELD, fieldName = "page.content", trim = true, key = "validation.required") })
	public String editme() {
		String content = page.getContent();
		page = pageManager.get(getUid());
		page.setContent(content);
		pageManager.save(page);
		addActionMessage(getText("save.success"));
		return JSON;
	}

	private List<LabelValue> suggestions;

	public List<LabelValue> getSuggestions() {
		return suggestions;
	}

	@JsonConfig(root = "suggestions")
	public String suggest() {
		if (StringUtils.isBlank(keyword))
			return NONE;
		Map<String, Integer> map = pageManager.findMatchedTags(keyword);
		suggestions = new ArrayList<LabelValue>(map.size());
		for (Map.Entry<String, Integer> entry : map.entrySet()) {
			LabelValue lv = new LabelValue();
			lv.setValue(entry.getKey());
			lv.setLabel(new StringBuilder(entry.getKey()).append("(")
					.append(entry.getValue()).append(")").toString());
			suggestions.add(lv);
		}
		return JSON;
	}

}