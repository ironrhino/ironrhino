package org.ironrhino.core.servlet;

import javax.servlet.ServletContext;
import javax.servlet.annotation.WebListener;

import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.ContextLoaderListener;

/**
 * 
 * Section 4.4 of the Servlet 3.0 specification does not permit configuration
 * methods to be called from a ServletContextListener that was not defined in
 * web.xml, a web-fragment.xml file nor annotated with @WebListener
 *
 */
@WebListener("Must declared as @WebListener according to Section 4.4")
public class MainContextLoaderListener extends ContextLoaderListener {

	public static final String CONFIG_LOCATION = "classpath*:resources/spring/applicationContext-*.xml";

	protected void configureAndRefreshWebApplicationContext(ConfigurableWebApplicationContext wac, ServletContext sc) {
		sc.setInitParameter(CONFIG_LOCATION_PARAM, CONFIG_LOCATION);
		super.configureAndRefreshWebApplicationContext(wac, sc);
	}

}
