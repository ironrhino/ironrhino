package org.ironrhino.common.support;

import java.io.IOException;
import java.io.StringReader;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.common.model.Page;
import org.ironrhino.common.service.PageManager;
import org.ironrhino.core.freemarker.FallbackTemplateProvider;
import org.ironrhino.core.struts.MyFreemarkerManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import freemarker.template.Configuration;
import freemarker.template.Template;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CmsTemplateProvider implements FallbackTemplateProvider {

	@Value("${view.ftl.location:" + MyFreemarkerManager.DEFAULT_FTL_LOCATION + "}")
	private String ftlLocation = MyFreemarkerManager.DEFAULT_FTL_LOCATION;

	@Value("${view.ftl.classpath:" + MyFreemarkerManager.DEFAULT_FTL_CLASSPATH + "}")
	private String ftlClasspath = MyFreemarkerManager.DEFAULT_FTL_CLASSPATH;

	@Autowired
	private PageManager pageManager;

	private Configuration configuration;

	@Override
	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}

	@Override
	public Template getTemplate(String name, Locale locale, String encoding, boolean parse) throws IOException {
		if (!name.startsWith("/"))
			name = "/" + name;
		String path = null;
		if (name.startsWith(ftlClasspath))
			path = name.substring(ftlClasspath.length());
		if (name.startsWith(ftlLocation))
			path = name.substring(ftlLocation.length());
		if (path != null) {
			Page page = pageManager.getByPath(path);
			if (page != null && StringUtils.isNotBlank(page.getContent()))
				return new Template(name, new StringReader(page.getContent()), configuration);
		}
		return null;
	}
}
