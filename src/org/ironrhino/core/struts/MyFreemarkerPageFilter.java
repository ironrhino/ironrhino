package org.ironrhino.core.struts;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;

import org.apache.struts2.sitemesh.OldDecorator2NewStrutsFreemarkerDecorator;
import org.apache.struts2.sitemesh.StrutsSiteMeshFactory;
import org.apache.struts2.views.freemarker.FreemarkerManager;
import org.ironrhino.core.spring.configuration.ResourcePresentConditional;
import org.springframework.stereotype.Component;

import com.opensymphony.module.sitemesh.Config;
import com.opensymphony.module.sitemesh.Factory;
import com.opensymphony.sitemesh.DecoratorSelector;
import com.opensymphony.sitemesh.webapp.SiteMeshFilter;
import com.opensymphony.sitemesh.webapp.SiteMeshWebAppContext;
import com.opensymphony.xwork2.inject.Inject;

@Component("sitemesh")
@ResourcePresentConditional("resources/sitemesh/sitemesh.xml")
public class MyFreemarkerPageFilter extends SiteMeshFilter {
	/*
	 * @see com.opensymphony.module.sitemesh.Factory.SITEMESH_FACTORY
	 */
	private static final String SITEMESH_FACTORY = "sitemesh.factory";

	@Inject(required = false)
	public static void setFreemarkerManager(FreemarkerManager mgr) {
		OldDecorator2NewStrutsFreemarkerDecorator.setFreemarkerManager(mgr);
	}

	protected FilterConfig filterConfig;

	@Override
	public void init(FilterConfig filterConfig) {
		this.filterConfig = filterConfig;
		super.init(filterConfig);
		ServletContext sc = filterConfig.getServletContext();
		Factory instance = (Factory) sc.getAttribute(SITEMESH_FACTORY);
		if (instance == null)
			sc.setAttribute(SITEMESH_FACTORY, new StrutsSiteMeshFactory(
					new Config(filterConfig)));
	}

	@Override
	protected DecoratorSelector initDecoratorSelector(
			SiteMeshWebAppContext webAppContext) {
		Factory factory = Factory.getInstance(new Config(filterConfig));
		return new MyFreemarkerMapper2DecoratorSelector(
				factory.getDecoratorMapper());
	}
}
