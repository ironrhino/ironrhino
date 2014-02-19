package org.ironrhino.common.support;

import java.io.IOException;
import java.io.StringReader;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.common.model.Page;
import org.ironrhino.common.service.PageManager;
import org.ironrhino.core.struts.FallbackTemplateProvider;
import org.ironrhino.core.struts.result.AutoConfigResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import freemarker.template.Configuration;
import freemarker.template.Template;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CmsTemplateProvider implements FallbackTemplateProvider {

	@Autowired
	private PageManager pageManager;

	private Configuration configuration;

	@Override
	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}

	@Override
	public Template getTemplate(String name, Locale locale, String encoding,
			boolean parse) throws IOException {
		if (!name.startsWith("/"))
			name = "/" + name;
		String path = null;
		if (name.startsWith(AutoConfigResult.DEFAULT_FTL_CLASSPATH))
			path = name.substring(AutoConfigResult.DEFAULT_FTL_CLASSPATH
					.length());
		if (name.startsWith(AutoConfigResult.DEFAULT_FTL_LOCATION))
			path = name.substring(AutoConfigResult.DEFAULT_FTL_LOCATION
					.length());
		if (path != null) {
			Page page = pageManager.getByPath(path);
			if (page != null && StringUtils.isNotBlank(page.getContent()))
				return new Template(name, new StringReader(page.getContent()),
						configuration);
		}
		return null;
	}
}
