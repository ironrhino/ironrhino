package org.ironrhino.core.struts.sitemesh;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts2.ServletActionContext;
import org.apache.struts2.dispatcher.Dispatcher;
import org.apache.struts2.views.freemarker.FreemarkerManager;

import com.opensymphony.module.sitemesh.Decorator;
import com.opensymphony.module.sitemesh.HTMLPage;
import com.opensymphony.sitemesh.Content;
import com.opensymphony.sitemesh.compatability.Content2HTMLPage;
import com.opensymphony.sitemesh.webapp.SiteMeshWebAppContext;
import com.opensymphony.sitemesh.webapp.decorator.BaseWebAppDecorator;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionEventListener;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.ActionProxy;
import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.LocaleProvider;
import com.opensymphony.xwork2.Result;
import com.opensymphony.xwork2.interceptor.PreResultListener;
import com.opensymphony.xwork2.util.ValueStack;
import com.opensymphony.xwork2.util.ValueStackFactory;

import freemarker.template.Configuration;
import freemarker.template.SimpleHash;
import freemarker.template.Template;

public class OldDecorator2NewStrutsFreemarkerDecorator extends BaseWebAppDecorator {

	private final FreemarkerManager freemarkerManager;

	private final Decorator oldDecorator;

	public OldDecorator2NewStrutsFreemarkerDecorator(Decorator oldDecorator, FreemarkerManager freemarkerManager) {
		this.oldDecorator = oldDecorator;
		this.freemarkerManager = freemarkerManager;
	}

	protected Locale getLocale(ActionInvocation invocation, Configuration configuration) {
		if (invocation.getAction() instanceof LocaleProvider) {
			return ((LocaleProvider) invocation.getAction()).getLocale();
		} else {
			return configuration.getLocale();
		}
	}

	@Override
	protected void render(Content content, HttpServletRequest request, HttpServletResponse response,
			ServletContext servletContext, SiteMeshWebAppContext webAppContext) throws IOException, ServletException {

		// see if the URI path (webapp) is set
		if (oldDecorator.getURIPath() != null) {
			// in a security conscious environment, the servlet container
			// may return null for a given URL
			if (servletContext.getContext(oldDecorator.getURIPath()) != null) {
				servletContext = servletContext.getContext(oldDecorator.getURIPath());
			}
		}

		ActionContext ctx = ServletActionContext.getActionContext(request);
		if (ctx == null) {
			// ok, one isn't associated with the request, so let's create one using the
			// current Dispatcher
			ValueStack vs = Dispatcher.getInstance().getContainer().getInstance(ValueStackFactory.class)
					.createValueStack();
			vs.getContext().putAll(Dispatcher.getInstance().createContextMap(request, response, null, servletContext));
			ctx = new ActionContext(vs.getContext());
			if (ctx.getActionInvocation() == null) {
				// put in a dummy ActionSupport so basic functionality still works
				ActionSupport action = new ActionSupport();
				vs.push(action);
				ctx.setActionInvocation(new DummyActionInvocation(action));
			}
		}

		// delegate to the actual page decorator
		render(content, request, response, servletContext, ctx);

	}

	protected void render(Content content, HttpServletRequest request, HttpServletResponse response,
			ServletContext servletContext, ActionContext ctx) throws ServletException, IOException {
		if (freemarkerManager == null) {
			throw new ServletException("Missing freemarker dependency");
		}

		try {
			// get the configuration and template
			Configuration config = freemarkerManager.getConfiguration(servletContext);
			Template template = config.getTemplate(oldDecorator.getPage(),
					getLocale(ctx.getActionInvocation(), config)); // WW-1181

			// get the main hash
			SimpleHash model = (SimpleHash) request.getAttribute(FreemarkerManager.ATTR_TEMPLATE_MODEL);
			if (model == null) {
				model = freemarkerManager.buildTemplateModel(ctx.getValueStack(), ctx.getActionInvocation().getAction(),
						servletContext, request, response, config.getObjectWrapper());
			}

			// populate the hash with the page
			HTMLPage htmlPage = new Content2HTMLPage(content, request);
			model.put("page", htmlPage);
			model.put("head", htmlPage.getHead());
			model.put("title", htmlPage.getTitle());
			model.put("body", htmlPage.getBody());
			model.put("page.properties", new SimpleHash(htmlPage.getProperties(), null));

			// finally, render it
			template.process(model, response.getWriter());
		} catch (Exception e) {
			String msg = "Error applying decorator to request: " + request.getRequestURL() + "?"
					+ request.getQueryString() + " with message:" + e.getMessage();
			throw new ServletException(msg, e);
		}
	}

	static class DummyActionInvocation implements ActionInvocation {

		private static final long serialVersionUID = -4808072199157363028L;

		ActionSupport action;

		public DummyActionInvocation(ActionSupport action) {
			this.action = action;
		}

		@Override
		public Object getAction() {
			return action;
		}

		@Override
		public boolean isExecuted() {
			return false;
		}

		@Override
		public ActionContext getInvocationContext() {
			return null;
		}

		@Override
		public ActionProxy getProxy() {
			return null;
		}

		@Override
		public Result getResult() throws Exception {
			return null;
		}

		@Override
		public String getResultCode() {
			return null;
		}

		@Override
		public void setResultCode(String resultCode) {
		}

		@Override
		public ValueStack getStack() {
			return null;
		}

		@Override
		public void addPreResultListener(PreResultListener listener) {
		}

		@Override
		public String invoke() throws Exception {
			return null;
		}

		@Override
		public String invokeActionOnly() throws Exception {
			return null;
		}

		@Override
		public void setActionEventListener(ActionEventListener listener) {
		}

		@Override
		public void init(ActionProxy proxy) {
		}

		@Override
		public ActionInvocation serialize() {
			return null;
		}

		@Override
		public ActionInvocation deserialize(ActionContext actionContext) {
			return null;
		}

	}

}
