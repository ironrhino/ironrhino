package org.ironrhino.common.action;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.common.model.Page;
import org.ironrhino.common.service.PageManager;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.model.ResultPage;
import org.ironrhino.core.search.SearchCriteria;
import org.ironrhino.core.struts.BaseAction;
import org.ironrhino.core.struts.sitemesh.RequestDecoratorMapper;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.interceptor.annotations.Before;

import lombok.Getter;
import lombok.Setter;

@AutoConfig(namespace = IssuePageAction.NAMESPACE, actionName = IssuePageAction.ACTION_NAME)
public class IssuePageAction extends BaseAction {

	private static final long serialVersionUID = -7189565572156313486L;

	public static final String NAMESPACE = "/";
	public static final String ACTION_NAME = "_issue_page_";

	@Autowired
	protected PageManager pageManager;

	private String name;

	@Getter
	@Setter
	protected ResultPage<Page> resultPage;

	@Getter
	protected Page page;

	@Getter
	protected Page previousPage;

	@Getter
	protected Page nextPage;

	public void setName(String name) {
		this.name = name;
		ActionContext.getContext().setName(getName());
	}

	public String getName() {
		if (name == null)
			name = ActionContext.getContext().getName();
		return name;
	}

	@Override
	public String execute() {
		if (resultPage == null)
			resultPage = new ResultPage<>();
		SearchCriteria criteria = new SearchCriteria();
		criteria.addSort("createDate", true);
		resultPage.setCriteria(criteria);
		resultPage = pageManager.findResultPageByTag(resultPage, getName());
		return "issuelist";
	}

	public String p() {
		String path = getUid();
		if (StringUtils.isNotBlank(path)) {
			path = '/' + path;
			page = pageManager.getByPath(path);
		}
		if (page == null)
			return NOTFOUND;
		Page[] p = pageManager.findPreviousAndNextPage(page, getName());
		previousPage = p[0];
		nextPage = p[1];
		return "issuepage";
	}

	@Before
	public void setDecorator() {
		RequestDecoratorMapper.setDecorator(getName());
	}
}
