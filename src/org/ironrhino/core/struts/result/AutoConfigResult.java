package org.ironrhino.core.struts.result;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.lang.model.SourceVersion;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.StrutsStatics;
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
		if (Action.SUCCESS.equals(resultCode) && !invocation.getProxy().getMethod().isEmpty()
				&& !invocation.getProxy().getMethod().equals("execute")) {
			String namespace = invocation.getProxy().getNamespace();
			String url = namespace + (namespace.endsWith("/") ? "" : "/") + invocation.getProxy().getActionName();
			response.sendRedirect(response.encodeRedirectURL(request.getContextPath() + url));
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
		HttpServletRequest request = (HttpServletRequest) invocation.getInvocationContext()
				.get(StrutsStatics.HTTP_REQUEST);
		String uri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
		if (uri != null) {
			uri = uri.substring(request.getContextPath().length());
			int index = uri.lastIndexOf('/');
			namespace = (index == 0 ? "/" : uri.substring(0, index));
		}
		String actionName = invocation.getInvocationContext().getName();
		if (namespace.equals("/"))
			namespace = "";
		String location = null;
		if (location == null && StringUtils.isNotBlank(styleHolder.get()))
			location = getTemplateLocation(getTemplateName(namespace, actionName, resultCode, true));
		if (location == null)
			location = getTemplateLocation(getTemplateName(namespace, actionName, resultCode, false));
		if (location == null && StringUtils.isNotBlank(styleHolder.get()))
			location = getTemplateLocation(getTemplateName(namespace, resultCode, true));
		if (location == null)
			location = getTemplateLocation(getTemplateName(namespace, resultCode, false));
		if (location == null && StringUtils.isNotBlank(styleHolder.get()))
			location = getTemplateLocation(getTemplateName(resultCode, true));
		if (location == null)
			location = getTemplateLocation(getTemplateName(resultCode, false));
		styleHolder.remove();
		if (location == null || location.contains("./"))
			throw new IllegalArgumentException("Location not found:" + location);
		return location;
	}

	public static String getTemplateLocation(String templateName) {
		templateName = FileUtils.normalizePath(templateName);
		String location = cache.get(templateName);
		if (location == null || AppInfo.getStage() == Stage.DEVELOPMENT) {
			ServletContext servletContext = ServletActionContext.getServletContext();
			if (freemarkerConfigurer == null)
				freemarkerConfigurer = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext)
						.getBean(FreemarkerConfigurer.class);
			String ftlClasspath = freemarkerConfigurer.getFtlClasspath();
			URL url = null;
			location = new StringBuilder().append(ftlClasspath).append(templateName).append(".ftl").toString();
			url = ClassLoaderUtil.getResource(location.substring(1), AutoConfigResult.class);
			if (url == null)
				location = "";
			cache.put(templateName, location);
		}
		return StringUtils.isEmpty(location) ? null : location;
	}

	// action level
	private String getTemplateName(String namespace, String actionName, String result, boolean withStyle) {
		StringBuilder sb = new StringBuilder();
		sb.append(namespace).append('/').append(actionName);
		if (!result.equals(Action.SUCCESS) && !result.equals(BaseAction.HOME))
			sb.append('_').append(result);
		if (withStyle && StringUtils.isNotBlank(styleHolder.get()))
			sb.append(".").append(styleHolder.get());
		return sb.toString();
	}

	// namespace level
	private String getTemplateName(String namespace, String result, boolean withStyle) {
		StringBuilder sb = new StringBuilder();
		sb.append(namespace).append('/').append(result);
		if (withStyle && StringUtils.isNotBlank(styleHolder.get()))
			sb.append(".").append(styleHolder.get());
		return sb.toString();
	}

	// application level
	private String getTemplateName(String result, boolean withStyle) {
		StringBuilder sb = new StringBuilder();
		sb.append("/meta/result/").append(result);
		if (withStyle && StringUtils.isNotBlank(styleHolder.get()))
			sb.append(".").append(styleHolder.get());
		return sb.toString();
	}
}
