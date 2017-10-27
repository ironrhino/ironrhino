package org.ironrhino.rest.doc.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.struts.BaseAction;
import org.ironrhino.core.util.ReflectionUtils;
import org.ironrhino.core.util.RequestUtils;
import org.ironrhino.rest.ApiConfigBase;
import org.ironrhino.rest.doc.ApiDoc;
import org.ironrhino.rest.doc.ApiDocInspector;
import org.ironrhino.rest.doc.ApiModuleObject;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.servlet.FrameworkServlet;

import lombok.Getter;
import lombok.Setter;

@AutoConfig
public class DocsAction extends BaseAction {

	private static final long serialVersionUID = -2983503425168586385L;

	@Autowired
	protected ServletContext servletContext;

	protected List<ApiConfigBase> apiConfigs;

	@Value("${apiBaseUrl:}")
	protected String apiBaseUrl;

	@Setter
	protected String version;

	@Getter
	@Setter
	protected String category = "";

	@Getter
	@Setter
	protected String module;

	@Getter
	@Setter
	protected String api;

	@Getter
	protected ApiDoc apiDoc;

	protected List<ApiConfigBase> getApiConfigs() {
		if (apiConfigs == null) {
			List<ApiConfigBase> temp = new ArrayList<>();
			Enumeration<String> names = servletContext.getAttributeNames();
			while (names.hasMoreElements()) {
				String name = names.nextElement();
				if (name.startsWith(FrameworkServlet.SERVLET_CONTEXT_PREFIX)) {
					ApplicationContext ctx = (ApplicationContext) servletContext.getAttribute(name);
					try {
						temp.add(ctx.getBean(ApiConfigBase.class));
					} catch (NoSuchBeanDefinitionException e) {

					}
				}
			}
			Collections.sort(temp, (o1, o2) -> o1.getVersion().compareTo(o2.getVersion()));
			apiConfigs = temp;
		}
		return apiConfigs;
	}

	protected ApiConfigBase getApiConfig() {
		List<ApiConfigBase> list = getApiConfigs();
		for (ApiConfigBase ac : getApiConfigs()) {
			if (ac.getVersion().equals(getVersion()))
				return ac;
		}
		return !list.isEmpty() ? list.get(list.size() - 1) : null;
	}

	public String getApiBaseUrl() {
		if (StringUtils.isBlank(apiBaseUrl))
			apiBaseUrl = RequestUtils.getBaseUrl(ServletActionContext.getRequest());
		String path = "/api";
		Map<String, ? extends ServletRegistration> map = servletContext.getServletRegistrations();
		ApiConfigBase ac = getApiConfig();
		for (ServletRegistration sr : map.values()) {
			if (ac != null && ReflectionUtils.getActualClass(ac).getName()
					.equals(sr.getInitParameter(ContextLoader.CONFIG_LOCATION_PARAM))) {
				String mapping = sr.getMappings().iterator().next();
				if (mapping.endsWith("/*")) {
					path = mapping.substring(0, mapping.lastIndexOf('/'));
					break;
				}
			}
		}
		return apiBaseUrl + path;
	}

	public String getVersion() {
		List<ApiConfigBase> list = getApiConfigs();
		if (version == null && !list.isEmpty()) {
			version = list.get(list.size() - 1).getVersion();
		}
		return version;
	}

	public Map<String, List<ApiModuleObject>> getApiModules() {
		Enumeration<String> names = servletContext.getAttributeNames();
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			if (name.startsWith(FrameworkServlet.SERVLET_CONTEXT_PREFIX)) {
				ApplicationContext ctx = (ApplicationContext) servletContext.getAttribute(name);
				try {
					ApiDocInspector adh = ctx.getBean(ApiDocInspector.class);
					return adh.getApiModules();
				} catch (NoSuchBeanDefinitionException e) {

				}
			}
		}
		return Collections.emptyMap();
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
