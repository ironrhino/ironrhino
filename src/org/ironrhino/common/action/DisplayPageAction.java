package org.ironrhino.common.action;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.ironrhino.common.model.Page;
import org.ironrhino.common.service.PageManager;
import org.ironrhino.common.support.SettingControl;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.security.role.UserRole;
import org.ironrhino.core.struts.BaseAction;
import org.ironrhino.core.util.AuthzUtils;
import org.springframework.beans.factory.annotation.Autowired;

@AutoConfig(namespace = DisplayPageAction.NAMESPACE, actionName = DisplayPageAction.ACTION_NAME)
public class DisplayPageAction extends BaseAction {

	private static final long serialVersionUID = -5865373753326653067L;

	public static final String NAMESPACE = "/";
	public static final String ACTION_NAME = "_display_page_";

	private Page page;

	private boolean preview;

	@Autowired
	private PageManager pageManager;

	@Autowired
	private SettingControl settingControl;

	public boolean isPreview() {
		return preview;
	}

	public void setPreview(boolean preview) {
		this.preview = preview;
	}

	public Page getPage() {
		return page;
	}

	@Override
	public String execute() {
		if (preview) {
			if (settingControl.getBooleanValue("cms.preview.open", false)
					|| AuthzUtils.getRoleNames().contains(UserRole.ROLE_ADMINISTRATOR)) {
				page = pageManager.getDraftByPath(getUid());
				if (StringUtils.isBlank(page.getContent())) {
					preview = false;
					page = null;
				}
			} else {
				preview = false;
			}
		}
		if (page == null)
			page = pageManager.getByPath(getUid());
		if (page == null) {
			ServletActionContext.getResponse().setStatus(404);
			return NOTFOUND;
		}
		return "page";
	}

}
