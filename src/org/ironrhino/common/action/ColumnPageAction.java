package org.ironrhino.common.action;

import org.springframework.beans.factory.annotation.Autowired;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.common.Constants;
import org.ironrhino.common.model.Page;
import org.ironrhino.common.service.PageManager;
import org.ironrhino.common.support.SettingControl;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.model.ResultPage;
import org.ironrhino.core.struts.BaseAction;
import org.ironrhino.core.struts.RequestDecoratorMapper;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.interceptor.annotations.Before;

@AutoConfig(namespace = ColumnPageAction.NAMESPACE, actionName = ColumnPageAction.ACTION_NAME)
public class ColumnPageAction extends BaseAction {

	private static final long serialVersionUID = -7189565572156313486L;

	public static final String NAMESPACE = "/";
	public static final String ACTION_NAME = "_column_page_";

	@Autowired
	protected PageManager pageManager;

	@Autowired
	protected SettingControl settingControl;

	protected String column;

	protected String[] columns;

	protected ResultPage<Page> resultPage;

	private String name;

	protected Page page;

	protected Page previousPage;

	protected Page nextPage;

	public ResultPage<Page> getResultPage() {
		return resultPage;
	}

	public void setResultPage(ResultPage<Page> resultPage) {
		this.resultPage = resultPage;
	}

	public String getColumn() {
		return column;
	}

	public void setColumn(String column) {
		this.column = column;
	}

	public String[] getColumns() {
		return columns;
	}

	public Page getPage() {
		return page;
	}

	public Page getPreviousPage() {
		return previousPage;
	}

	public Page getNextPage() {
		return nextPage;
	}

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
		columns = settingControl
				.getStringArray(Constants.SETTING_KEY_CMS_PREFIX + getName()
						+ Constants.SETTING_KEY_CMS_COLUMN_SUFFIX);
		column = getUid();
		if (StringUtils.isBlank(column)) {
			page = pageManager.getByPath("/" + getName() + "/preface");
			if (page != null)
				return "columnpage";
			// if (columns != null && columns.length > 0)
			// column = columns[0];
		}
		if (resultPage == null)
			resultPage = new ResultPage<Page>();
		if (StringUtils.isBlank(column))
			resultPage = pageManager.findResultPageByTag(resultPage, getName());
		else
			resultPage = pageManager.findResultPageByTag(resultPage,
					new String[] { getName(), column });
		return "columnlist";
	}

	public String p() {
		columns = settingControl
				.getStringArray(Constants.SETTING_KEY_CMS_PREFIX + getName()
						+ Constants.SETTING_KEY_CMS_COLUMN_SUFFIX);
		String path = getUid();
		if (StringUtils.isNotBlank(path)) {
			path = "/" + path;
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
