package org.ironrhino.core.struts.sitemesh;

import java.io.IOException;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.StrutsStatics;
import org.apache.struts2.views.freemarker.FreemarkerManager;
import org.ironrhino.core.spring.configuration.ResourcePresentConditional;
import org.ironrhino.core.tracing.Tracing;
import org.springframework.stereotype.Component;

import com.opensymphony.module.sitemesh.Config;
import com.opensymphony.module.sitemesh.Factory;
import com.opensymphony.module.sitemesh.factory.DefaultFactory;
import com.opensymphony.sitemesh.Content;
import com.opensymphony.sitemesh.ContentProcessor;
import com.opensymphony.sitemesh.Decorator;
import com.opensymphony.sitemesh.DecoratorSelector;
import com.opensymphony.sitemesh.compatability.PageParser2ContentProcessor;
import com.opensymphony.sitemesh.webapp.ContainerTweaks;
import com.opensymphony.sitemesh.webapp.ContentBufferingResponse;
import com.opensymphony.sitemesh.webapp.SiteMeshWebAppContext;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.inject.Inject;

@Component("sitemeshFilter")
@ResourcePresentConditional("resources/sitemesh/sitemesh.xml")
public class SitemeshFilter implements Filter {

	private static final String SITEMESH_FACTORY = "sitemesh.factory";

	private static final String ALREADY_APPLIED_KEY = "sitemesh.APPLIED_ONCE";

	private ContainerTweaks containerTweaks;

	private static FreemarkerManager freemarkerManager;

	protected FilterConfig filterConfig;

	@Inject(required = false)
	public static void setFreemarkerManager(FreemarkerManager mgr) {
		freemarkerManager = mgr;
	}

	@Override
	public void init(FilterConfig filterConfig) {
		this.filterConfig = filterConfig;
		containerTweaks = new ContainerTweaks();
		ServletContext sc = filterConfig.getServletContext();
		Factory instance = (Factory) sc.getAttribute(SITEMESH_FACTORY);
		if (instance == null)
			sc.setAttribute(SITEMESH_FACTORY, new StrutsSiteMeshFactory(new Config(filterConfig)));
	}

	@Override
	public void destroy() {
		filterConfig = null;
		containerTweaks = null;
	}

	@Override
	public void doFilter(ServletRequest rq, ServletResponse rs, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) rq;
		HttpServletResponse response = (HttpServletResponse) rs;

		if (filterAlreadyAppliedForRequest(request)) {
			chain.doFilter(request, response);
			return;
		}

		ServletContext servletContext = filterConfig.getServletContext();
		SiteMeshWebAppContext webAppContext = new SiteMeshWebAppContext(request, response, servletContext);
		ContentProcessor contentProcessor = initContentProcessor(webAppContext);
		DecoratorSelector decoratorSelector = initDecoratorSelector(webAppContext);

		if (!contentProcessor.handles(webAppContext)) {
			chain.doFilter(request, response);
			return;
		}

		try {
			ContentBufferingResponse contentBufferingResponse = new ContentBufferingResponse(response, contentProcessor,
					webAppContext);
			chain.doFilter(request, contentBufferingResponse);
			webAppContext.setUsingStream(contentBufferingResponse.isUsingStream());
			Content content = contentBufferingResponse.getContent();
			if (content == null) {
				if (request.isAsyncStarted()) {
					request.getAsyncContext().addListener(new AsyncListener() {

						@Override
						public void onTimeout(AsyncEvent event) throws IOException {

						}

						@Override
						public void onStartAsync(AsyncEvent event) throws IOException {

						}

						@Override
						public void onError(AsyncEvent event) throws IOException {

						}

						@Override
						public void onComplete(AsyncEvent event) throws IOException {
							Content con = contentBufferingResponse.getContent();
							if (con == null)
								return;
							Decorator decorator = decoratorSelector.selectDecorator(con, webAppContext);
							decorator.render(con, webAppContext);
						}
					});
				}
				return;
			}

			Decorator decorator = decoratorSelector.selectDecorator(content, webAppContext);
			Tracing.execute(decorator.getClass().getName() + ".render(Content,SiteMeshContext)", () -> {
				decorator.render(content, webAppContext);
			}, "component", "decorator");

		} catch (IllegalStateException e) {
			if (!containerTweaks.shouldIgnoreIllegalStateExceptionOnErrorPage()) {
				throw e;
			}
		} catch (RuntimeException e) {
			if (containerTweaks.shouldLogUnhandledExceptions()) {
				servletContext.log("Unhandled exception occurred whilst decorating page", e);
			}
			throw e;
		} catch (ServletException e) {
			request.setAttribute(ALREADY_APPLIED_KEY, null);
			throw e;
		}
	}

	private ContentProcessor initContentProcessor(SiteMeshWebAppContext webAppContext) {
		Factory factory = Factory.getInstance(new Config(filterConfig));
		factory.refresh();
		return new PageParser2ContentProcessor(factory);
	}

	private DecoratorSelector initDecoratorSelector(SiteMeshWebAppContext webAppContext) {
		Factory factory = Factory.getInstance(new Config(filterConfig));
		return new MyFreemarkerMapper2DecoratorSelector(factory.getDecoratorMapper(), freemarkerManager);
	}

	private boolean filterAlreadyAppliedForRequest(HttpServletRequest request) {
		if (request.getAttribute(ALREADY_APPLIED_KEY) == Boolean.TRUE) {
			return true;
		}
		request.setAttribute(ALREADY_APPLIED_KEY, Boolean.TRUE);
		return false;
	}

	private static class StrutsSiteMeshFactory extends DefaultFactory {

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
