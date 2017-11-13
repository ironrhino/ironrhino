package org.ironrhino.core.freemarker;

import java.io.IOException;
import java.util.Locale;

import org.apache.struts2.ServletActionContext;
import org.apache.struts2.views.freemarker.FreemarkerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.inject.Container;

import freemarker.template.Configuration;
import freemarker.template.Template;

@Component
public class TemplateProvider {

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private FreemarkerConfigurer freemarkerConfigurer;

	private Configuration configuration;

	private Configuration getConfiguration() {
		if (configuration == null) {
			try {
				Container con = ActionContext.getContext().getContainer();
				FreemarkerManager freemarkerManager = con
						.getInstance(org.apache.struts2.views.freemarker.FreemarkerManager.class);
				configuration = freemarkerManager.getConfiguration(ServletActionContext.getServletContext());
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
		return configuration;
	}

	public boolean isTemplatePresent(String name) {
		try {
			return getConfiguration().getTemplate(name) != null;
		} catch (IOException e) {
			return false;
		}
	}

	public Template getTemplate(String name) throws IOException {
		Locale loc = getConfiguration().getLocale();
		return getTemplate(name, loc, getConfiguration().getEncoding(loc), true);
	}

	public Template getTemplate(String name, Locale locale, String encoding) throws IOException {
		return getTemplate(name, locale, encoding, true);
	}

	public Template getTemplate(String name, Locale locale) throws IOException {
		return getTemplate(name, locale, getConfiguration().getEncoding(locale), true);
	}

	public Template getTemplate(String name, String encoding) throws IOException {
		return getTemplate(name, getConfiguration().getLocale(), encoding, true);
	}

	public Template getTemplate(String name, Locale locale, String encoding, boolean parse) throws IOException {
		String ftlClasspath = freemarkerConfigurer.getFtlClasspath();
		if (name.startsWith(ftlClasspath))
			return getConfiguration().getTemplate(name, locale, encoding, parse);
		String templateName = ftlClasspath + (name.indexOf('/') != 0 ? "/" : "") + name;
		return getConfiguration().getTemplate(templateName, locale, encoding, parse);
	}

}
