package org.ironrhino.core.struts.result;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.lang.model.SourceVersion;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.StrutsStatics;
import org.apache.struts2.views.freemarker.FreemarkerResult;
import org.ironrhino.core.freemarker.FreemarkerConfigurer;
import org.ironrhino.core.struts.BaseAction;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.AppInfo.Stage;
import org.ironrhino.core.util.FileUtils;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.opensymphony.xwork2.Action;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.util.ClassLoaderUtil;

public class AutoConfigResult extends FreemarkerResult {

	private static final long serialVersionUID = -2277156996891287055L;

	private static ThreadLocal<String> styleHolder = new ThreadLocal<>();

	private volatile static FreemarkerConfigurer freemarkerConfigurer;

	public static void setStyle(String style) {
		styleHolder.set(style);
	}

	@Override
	public void execute(ActionInvocation invocation) throws Exception {
		String resultCode = invocation.getResultCode();
		if (Action.NONE.equals(resultCode))
			return;
		ActionContext ctx = invocation.getInvocationContext();
		HttpServletRequest request = (HttpServletRequest) ctx.get(StrutsStatics.HTTP_REQUEST);
		HttpServletResponse response = (HttpServletResponse) ctx.get(StrutsStatics.HTTP_RESPONSE);
		if (Action.SUCCESS.equals(resultCode) && !invocation.getProxy().getMethod().equals("")
				&& !invocation.getProxy().getMethod().equals("execute")) {
			String namespace = invocation.getProxy().getNamespace();
			String url = namespace + (namespace.endsWith("/") ? "" : "/") + invocation.getProxy().getActionName();
			response.sendRedirect(request.getContextPath() + url);
			return;
		}
		if (!"XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))) {
			if (Action.ERROR.equals(resultCode))
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			if (BaseAction.NOTFOUND.equals(resultCode))
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
		String finalLocation = conditionalParse(location, invocation);
		doExecute(finalLocation, invocation);
	}

	private static Map<String, String> cache = new ConcurrentHashMap<>(256);

	@Override
	protected String conditionalParse(String param, ActionInvocation invocation) {
		String resultCode = invocation.getResultCode();
		if (resultCode == null || !SourceVersion.isIdentifier(resultCode))
			throw new IllegalArgumentException("Result code must be legal java identifier");
		String namespace = invocation.getProxy().getNamespace();
		String actionName = invocation.getInvocationContext().getName();
		if (namespace.equals("/"))
			namespace = "";
		String templateName = null;
		String location = null;
		if (StringUtils.isNotBlank(styleHolder.get())) {
			templateName = getTemplateName(namespace, actionName, resultCode, true);
			location = getTemplateLocation(templateName);
		}
		if (location == null) {
			templateName = getTemplateName(namespace, actionName, resultCode, false);
			location = cache.get(templateName);
			if (location == null || AppInfo.getStage() == Stage.DEVELOPMENT) {
				ServletContext servletContext = ServletActionContext.getServletContext();
				if (freemarkerConfigurer == null)
					freemarkerConfigurer = WebApplicationContextUtils.getWebApplicationContext(servletContext)
							.getBean(FreemarkerConfigurer.class);
				String ftlLocation = freemarkerConfigurer.getFtlLocation();
				String ftlClasspath = freemarkerConfigurer.getFtlClasspath();
				URL url = null;
				location = getTemplateLocation(templateName);
				if (location == null) {
					if (StringUtils.isNotBlank(styleHolder.get())) {
						location = new StringBuilder().append(ftlLocation).append("/meta/result/").append(resultCode)
								.append(".").append(styleHolder.get()).append(".ftl").toString();
						try {
							url = servletContext.getResource(location);
						} catch (MalformedURLException e) {
							e.printStackTrace();
						}
					}
					if (url == null) {
						location = new StringBuilder().append(ftlLocation).append("/meta/result/").append(resultCode)
								.append(".ftl").toString();
						try {
							url = servletContext.getResource(location);
						} catch (MalformedURLException e) {
							e.printStackTrace();
						}
					}
					if (url == null && StringUtils.isNotBlank(styleHolder.get())) {
						location = new StringBuilder().append(ftlClasspath).append("/meta/result/").append(resultCode)
								.append(".").append(styleHolder.get()).append(".ftl").toString();
						url = ClassLoaderUtil.getResource(location.substring(1), AutoConfigResult.class);
					}
					if (url == null)
						location = new StringBuilder().append(ftlClasspath).append("/meta/result/").append(resultCode)
								.append(".ftl").toString();
				}
				cache.put(templateName, location);
			}
		}
		styleHolder.remove();
		if (location.contains("./"))
			throw new IllegalArgumentException("Location must be absolute");
		return location;
	}

	public static String getTemplateLocation(String templateName) {
		templateName = FileUtils.normalizePath(templateName);
		String location = cache.get(templateName);
		if (location == null || AppInfo.getStage() == Stage.DEVELOPMENT) {
			ServletContext servletContext = ServletActionContext.getServletContext();
			if (freemarkerConfigurer == null)
				freemarkerConfigurer = WebApplicationContextUtils.getWebApplicationContext(servletContext)
						.getBean(FreemarkerConfigurer.class);
			String ftlLocation = freemarkerConfigurer.getFtlLocation();
			String ftlClasspath = freemarkerConfigurer.getFtlClasspath();
			URL url = null;
			location = new StringBuilder().append(ftlLocation).append(templateName).append(".ftl").toString();
			try {
				url = servletContext.getResource(location);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			if (url == null) {
				location = new StringBuilder().append(ftlClasspath).append(templateName).append(".ftl").toString();
				url = ClassLoaderUtil.getResource(location.substring(1), AutoConfigResult.class);
			}
			if (url == null)
				location = "";
			cache.put(templateName, location);
		}
		return StringUtils.isEmpty(location) ? null : location;
	}

	private String getTemplateName(String namespace, String actionName, String result, boolean withStyle) {
		StringBuilder sb = new StringBuilder();
		sb.append(namespace).append('/').append(actionName);
		if (!result.equals(Action.SUCCESS) && !result.equals(BaseAction.HOME))
			sb.append('_').append(result);
		if (withStyle && StringUtils.isNotBlank(styleHolder.get()))
			sb.append(".").append(styleHolder.get());
		return sb.toString();
	}
}
