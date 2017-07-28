package org.ironrhino.core.struts.sitemesh;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.views.freemarker.FreemarkerManager;
import org.ironrhino.core.util.HtmlUtils;

import com.opensymphony.module.sitemesh.Decorator;
import com.opensymphony.sitemesh.Content;
import com.opensymphony.xwork2.ActionContext;

public class MyOldDecorator2NewStrutsFreemarkerDecorator extends OldDecorator2NewStrutsFreemarkerDecorator {

	public static final String X_FRAGMENT = "X-Fragment";
	public static final String X_EXACT_FRAGMENT = "X-Exact-Fragment";

	public MyOldDecorator2NewStrutsFreemarkerDecorator(Decorator oldDecorator, FreemarkerManager freemarkerManager) {
		super(oldDecorator, freemarkerManager);
	}

	@Override
	protected void render(Content content, HttpServletRequest request, HttpServletResponse response,
			ServletContext servletContext, ActionContext ctx) throws ServletException, IOException {
		String replacement = request.getHeader(X_FRAGMENT);
		if (StringUtils.isNotBlank(replacement)) {
			if ("_".equals(replacement)) {
				Writer writer = response.getWriter();
				writer.append("<title>").append(content.getTitle()).append("</title>");
				content.writeBody(writer);
				writer.flush();
				return;
			} else {
				StringWriter writer = new StringWriter();
				content.writeBody(writer);
				String[] ids = replacement.split(",");
				String compressed = HtmlUtils.compress(writer.toString(), ids);
				if (compressed == null) {
					super.render(content, request, response, servletContext, ctx);
				} else if (compressed.length() == 0) {
					if (request.getHeader(X_EXACT_FRAGMENT) != null) {
						StringBuilder sb = new StringBuilder();
						for (String id : ids)
							sb.append("<div id=\"").append(id).append("\"></div>");
						compressed = sb.toString();
						response.getWriter().write(compressed);
						response.getWriter().flush();
						return;
					} else {
						super.render(content, request, response, servletContext, ctx);
					}
				} else {
					response.getWriter().write(compressed);
					response.getWriter().flush();
					return;
				}
			}
		} else {
			super.render(content, request, response, servletContext, ctx);
		}

	}

}
