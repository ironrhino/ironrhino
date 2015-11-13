package org.ironrhino.rest.doc.action;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.struts.BaseAction;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.AppInfo.Stage;
import org.ironrhino.core.util.RequestUtils;
import org.ironrhino.rest.doc.ApiDoc;
import org.ironrhino.rest.doc.ApiDocHelper;
import org.ironrhino.rest.doc.ApiModuleObject;
import org.springframework.beans.factory.annotation.Value;

@AutoConfig
public class DocsAction extends BaseAction {

	private static final long serialVersionUID = -2983503425168586385L;

	protected static Map<String, List<ApiModuleObject>> cache;

	@Value("${apiBaseUrl:}")
	protected String apiBaseUrl;

	protected String category = "";

	protected String module;

	protected String api;

	protected ApiDoc apiDoc;

	public String getApiBaseUrl() {
		if (StringUtils.isBlank(apiBaseUrl)) {
			apiBaseUrl = RequestUtils.getBaseUrl(ServletActionContext.getRequest()) + "/api";
		}
		return apiBaseUrl;
	}

	public Map<String, List<ApiModuleObject>> getApiModules() {
		if (cache == null || AppInfo.getStage() == Stage.DEVELOPMENT) {
			cache = ApiDocHelper.getApiModules();
		}
		return cache;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getModule() {
		return module;
	}

	public void setModule(String module) {
		this.module = module;
	}

	public String getApi() {
		return api;
	}

	public void setApi(String api) {
		this.api = api;
	}

	public ApiDoc getApiDoc() {
		return apiDoc;
	}

	@Override
	public String execute() {
		if (StringUtils.isNotBlank(module) && StringUtils.isNotBlank(api)) {
			List<ApiModuleObject> list = getApiModules().get(category);
			if (list != null)
				loop: for (ApiModuleObject apiModule : list) {
					if (module.equals(apiModule.getName())) {
						for (ApiDoc doc : apiModule.getApiDocs()) {
							if (api.equals(doc.getName())) {
								apiDoc = doc;
								break loop;
							}
						}
					}
				}
		}
		return SUCCESS;
	}

}
