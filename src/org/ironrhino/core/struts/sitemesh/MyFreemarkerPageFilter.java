package org.ironrhino.core.struts.sitemesh;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.StrutsStatics;
import org.apache.struts2.views.freemarker.FreemarkerManager;
import org.ironrhino.core.spring.configuration.ResourcePresentConditional;
import org.springframework.stereotype.Component;

import com.opensymphony.module.sitemesh.Config;
import com.opensymphony.module.sitemesh.Factory;
import com.opensymphony.module.sitemesh.factory.DefaultFactory;
import com.opensymphony.sitemesh.DecoratorSelector;
import com.opensymphony.sitemesh.webapp.SiteMeshFilter;
import com.opensymphony.sitemesh.webapp.SiteMeshWebAppContext;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.inject.Inject;

@Component("sitemeshFilter")
@ResourcePresentConditional("resources/sitemesh/sitemesh.xml")
public class MyFreemarkerPageFilter extends SiteMeshFilter {

	private static final String SITEMESH_FACTORY = "sitemesh.factory";

	private static FreemarkerManager freemarkerManager;

	protected FilterConfig filterConfig;

	@Inject(required = false)
	public static void setFreemarkerManager(FreemarkerManager mgr) {
		freemarkerManager = mgr;
	}

	@Override
	public void init(FilterConfig filterConfig) {
		this.filterConfig = filterConfig;
		super.init(filterConfig);
		ServletContext sc = filterConfig.getServletContext();
		Factory instance = (Factory) sc.getAttribute(SITEMESH_FACTORY);
		if (instance == null)
			sc.setAttribute(SITEMESH_FACTORY, new StrutsSiteMeshFactory(new Config(filterConfig)));
	}

	@Override
	protected DecoratorSelector initDecoratorSelector(SiteMeshWebAppContext webAppContext) {
		Factory factory = Factory.getInstance(new Config(filterConfig));
		return new MyFreemarkerMapper2DecoratorSelector(factory.getDecoratorMapper(), freemarkerManager);
	}

	static class StrutsSiteMeshFactory extends DefaultFactory {

		public StrutsSiteMeshFactory(Config config) {
			super(config);
		}

		@Override
		public boolean shouldParsePage(String contentType) {
			return !isInsideActionTag() && super.shouldParsePage(contentType);
		}

		private boolean isInsideActionTag() {
			if (ActionContext.getContext() == null)
				return false;
			Object attribute = ServletActionContext.getRequest()
					.getAttribute(StrutsStatics.STRUTS_ACTION_TAG_INVOCATION);
			return (Boolean) ObjectUtils.defaultIfNull(attribute, Boolean.FALSE);
		}
	}

}
