package org.ironrhino.core.struts;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.struts2.ServletActionContext;

import com.opensymphony.module.sitemesh.Config;
import com.opensymphony.module.sitemesh.Decorator;
import com.opensymphony.module.sitemesh.DecoratorMapper;
import com.opensymphony.module.sitemesh.Page;
import com.opensymphony.module.sitemesh.mapper.AbstractDecoratorMapper;
import com.opensymphony.module.sitemesh.mapper.DefaultDecorator;

public class RequestDecoratorMapper extends AbstractDecoratorMapper {

	private ServletContext sc;

	private String decoratorParameter = "decorator";

	@Override
	public void init(Config config, Properties properties,
			DecoratorMapper parent) throws InstantiationException {
		super.init(config, properties, parent);
		sc = config.getServletContext();
		sc.setAttribute(this.getClass().getName(), this);
		decoratorParameter = properties.getProperty("decorator.parameter",
				"decorator");
	}

	@Override
	public Decorator getDecorator(HttpServletRequest request, Page page) {
		Decorator result = null;
		Object attr = request.getAttribute(decoratorParameter);
		if (attr instanceof String) {
			String decorator = (String) attr;
			result = getNamedDecorator(request, decorator);
			if (result == null) {
				String location = "/WEB-INF/view/ftl/decorator/" + decorator
						+ ".ftl";
				URL url = null;
				try {
					url = config.getServletContext().getResource(location);
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
				if (url == null) {
					location = "/resources/view/decorator/" + decorator
							+ ".ftl";
				}
				result = new DefaultDecorator(decorator, location, null);
			}
		}
		return result == null ? super.getDecorator(request, page) : result;
	}

	public void setDecorator(HttpServletRequest request, String name) {
		Decorator result = getNamedDecorator(request, name);
		if (result != null)
			request.setAttribute(decoratorParameter, name);
	}

	public static void setDecorator(String name) {
		RequestDecoratorMapper rdm = (RequestDecoratorMapper) ServletActionContext
				.getServletContext().getAttribute(
						RequestDecoratorMapper.class.getName());
		if (rdm != null)
			rdm.setDecorator(ServletActionContext.getRequest(), name);
	}
}
