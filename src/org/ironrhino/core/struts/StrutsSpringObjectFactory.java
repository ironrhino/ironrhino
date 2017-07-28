package org.ironrhino.core.struts;

import com.opensymphony.xwork2.inject.Container;
import com.opensymphony.xwork2.inject.Inject;
import com.opensymphony.xwork2.spring.SpringObjectFactory;
import com.opensymphony.xwork2.util.logging.Logger;
import com.opensymphony.xwork2.util.logging.LoggerFactory;
import org.apache.struts2.StrutsConstants;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.ServletContext;

public class StrutsSpringObjectFactory extends SpringObjectFactory {

	private static final long serialVersionUID = 1L;

	private static final Logger LOG = LoggerFactory.getLogger(StrutsSpringObjectFactory.class);

	@Inject
	public StrutsSpringObjectFactory(
			@Inject(value = StrutsConstants.STRUTS_OBJECTFACTORY_SPRING_AUTOWIRE, required = false) String autoWire,
			@Inject(value = StrutsConstants.STRUTS_OBJECTFACTORY_SPRING_AUTOWIRE_ALWAYS_RESPECT, required = false) String alwaysAutoWire,
			@Inject(value = StrutsConstants.STRUTS_OBJECTFACTORY_SPRING_USE_CLASS_CACHE, required = false) String useClassCacheStr,
			@Inject ServletContext servletContext, @Inject Container container) {

		super(container);
		boolean useClassCache = "true".equals(useClassCacheStr);
		if (LOG.isInfoEnabled()) {
			LOG.info("Initializing Struts-Spring integration...");
		}

		Object rootWebApplicationContext = servletContext
				.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);

		ApplicationContext appContext = (ApplicationContext) rootWebApplicationContext;

		this.setApplicationContext(appContext);

		int type = AutowireCapableBeanFactory.AUTOWIRE_BY_NAME; // default
		if ("name".equals(autoWire)) {
			type = AutowireCapableBeanFactory.AUTOWIRE_BY_NAME;
		} else if ("type".equals(autoWire)) {
			type = AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE;
		} else if ("constructor".equals(autoWire)) {
			type = AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR;
		} else if ("no".equals(autoWire)) {
			type = AutowireCapableBeanFactory.AUTOWIRE_NO;
		}
		this.setAutowireStrategy(type);

		this.setUseClassCache(useClassCache);

		this.setAlwaysRespectAutowireStrategy("true".equalsIgnoreCase(alwaysAutoWire));

		if (LOG.isInfoEnabled()) {
			LOG.info("... initialized Struts-Spring integration successfully");
		}
	}
}
