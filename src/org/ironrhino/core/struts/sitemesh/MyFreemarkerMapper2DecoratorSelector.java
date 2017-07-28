package org.ironrhino.core.struts.sitemesh;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts2.views.freemarker.FreemarkerManager;

import com.opensymphony.module.sitemesh.DecoratorMapper;
import com.opensymphony.sitemesh.Content;
import com.opensymphony.sitemesh.DecoratorSelector;
import com.opensymphony.sitemesh.SiteMeshContext;
import com.opensymphony.sitemesh.compatability.Content2HTMLPage;
import com.opensymphony.sitemesh.webapp.SiteMeshWebAppContext;
import com.opensymphony.sitemesh.webapp.decorator.NoDecorator;

public class MyFreemarkerMapper2DecoratorSelector implements DecoratorSelector {

	private final DecoratorMapper decoratorMapper;

	private final FreemarkerManager freemarkerManager;

	public MyFreemarkerMapper2DecoratorSelector(DecoratorMapper decoratorMapper, FreemarkerManager freemarkerManager) {
		this.decoratorMapper = decoratorMapper;
		this.freemarkerManager = freemarkerManager;
	}

	@Override
	public com.opensymphony.sitemesh.Decorator selectDecorator(Content content, SiteMeshContext context) {
		SiteMeshWebAppContext webAppContext = (SiteMeshWebAppContext) context;
		HttpServletRequest request = webAppContext.getRequest();
		com.opensymphony.module.sitemesh.Decorator decorator = decoratorMapper.getDecorator(request,
				new Content2HTMLPage(content, request));
		if (decorator == null || decorator.getPage() == null) {
			return new NoDecorator();
		} else {
			return new MyOldDecorator2NewStrutsFreemarkerDecorator(decorator, freemarkerManager);
		}
	}
}
