package org.ironrhino.rest.doc.action;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.struts.BaseAction;
import org.ironrhino.core.util.RequestUtils;
import org.ironrhino.rest.doc.ApiDoc;
import org.ironrhino.rest.doc.ApiDocHelper;
import org.ironrhino.rest.doc.ApiModuleObject;
import org.springframework.beans.factory.annotation.Value;

@AutoConfig
public class DocsAction extends BaseAction {

	private static final long serialVersionUID = -2983503425168586385L;

	private List<ApiModuleObject> apiModules;

	@Value("${apiBaseUrl:}")
	private String apiBaseUrl;

	private String module;

	private String api;

	private ApiDoc apiDoc;

	public String getApiBaseUrl() {
		return apiBaseUrl;
	}

	public List<ApiModuleObject> getApiModules() {
		return apiModules;
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

	public String execute() {
		apiModules = ApiDocHelper.getApiModules();
		if (StringUtils.isBlank(apiBaseUrl)) {
			apiBaseUrl = RequestUtils.getBaseUrl(ServletActionContext
					.getRequest()) + "/api";
		}
		if (StringUtils.isNotBlank(module) && StringUtils.isNotBlank(api)) {
			loop: for (ApiModuleObject apiModule : apiModules) {
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
