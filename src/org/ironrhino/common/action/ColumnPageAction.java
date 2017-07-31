package org.ironrhino.common.action;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.common.model.Page;
import org.ironrhino.common.service.PageManager;
import org.ironrhino.common.support.CmsActionMappingMatcher;
import org.ironrhino.common.support.SettingControl;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.model.ResultPage;
import org.ironrhino.core.struts.BaseAction;
import org.ironrhino.core.struts.sitemesh.RequestDecoratorMapper;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.interceptor.annotations.Before;

import lombok.Getter;
import lombok.Setter;

@AutoConfig(namespace = ColumnPageAction.NAMESPACE, actionName = ColumnPageAction.ACTION_NAME)
public class ColumnPageAction extends BaseAction {

	private static final long serialVersionUID = -7189565572156313486L;

	public static final String NAMESPACE = "/";
	public static final String ACTION_NAME = "_column_page_";

	@Autowired
	protected PageManager pageManager;

	@Autowired
	protected SettingControl settingControl;

	private String name;

	@Getter
	@Setter
	protected String column;

	@Getter
	protected String[] columns;

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
		return list();
	}

	public String list() {
		columns = settingControl.getStringArray(CmsActionMappingMatcher.SETTING_KEY_CMS_PREFIX + getName()
				+ CmsActionMappingMatcher.SETTING_KEY_CMS_COLUMN_SUFFIX);
		column = getUid();
		if (StringUtils.isBlank(column)) {
			page = pageManager.getByPath("/" + getName() + "/preface");
			if (page != null)
				return "columnpage";
			// if (columns != null && columns.length > 0)
			// column = columns[0];
		}
		if (resultPage == null)
			resultPage = new ResultPage<>();
		if (StringUtils.isBlank(column))
			resultPage = pageManager.findResultPageByTag(resultPage, getName());
		else
			resultPage = pageManager.findResultPageByTag(resultPage, new String[] { getName(), column });
		return "columnlist";
	}

	public String p() {
		columns = settingControl.getStringArray(CmsActionMappingMatcher.SETTING_KEY_CMS_PREFIX + getName()
				+ CmsActionMappingMatcher.SETTING_KEY_CMS_COLUMN_SUFFIX);
		String path = getUid();
		if (StringUtils.isNotBlank(path)) {
			path = '/' + path;
			page = pageManager.getByPath(path);
		}
		if (page == null)
			return NOTFOUND;
		Page[] p = pageManager.findPreviousAndNextPage(page, getName(), column);
		previousPage = p[0];
		nextPage = p[1];
		return "columnpage";
	}

	@Before
	public void setDecorator() {
		RequestDecoratorMapper.setDecorator(getName());
	}
}
