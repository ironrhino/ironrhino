package org.ironrhino.core.struts.result;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.StrutsStatics;
import org.apache.struts2.views.freemarker.FreemarkerResult;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.AppInfo.Stage;

import com.opensymphony.xwork2.Action;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.inject.Inject;
import com.opensymphony.xwork2.util.ClassLoaderUtil;

public class AutoConfigResult extends FreemarkerResult {

	private static final long serialVersionUID = -2277156996891287055L;

	public static final String DEFAULT_FTL_LOCATION = "/WEB-INF/view/ftl";

	public static final String DEFAULT_FTL_CLASSPATH = "/resources/view";

	@Inject(value = "ironrhino.view.ftl.location", required = false)
	private static String ftlLocation = DEFAULT_FTL_LOCATION;

	@Inject(value = "ironrhino.view.ftl.classpath", required = false)
	private static String ftlClasspath = DEFAULT_FTL_CLASSPATH;

	private static ThreadLocal<String> styleHolder = new ThreadLocal<String>();

	public static void setStyle(String style) {
		styleHolder.set(style);
	}

	@Override
	public void execute(ActionInvocation invocation) throws Exception {
		if (invocation.getResultCode().equals(Action.NONE))
			return;
		if (invocation.getResultCode().equals(Action.SUCCESS)
				&& !invocation.getProxy().getMethod().equals("")
				&& !invocation.getProxy().getMethod().equals("execute")) {
			ActionContext ctx = invocation.getInvocationContext();
			HttpServletRequest request = (HttpServletRequest) ctx
					.get(StrutsStatics.HTTP_REQUEST);
			HttpServletResponse response = (HttpServletResponse) ctx
					.get(StrutsStatics.HTTP_RESPONSE);
			String namespace = invocation.getProxy().getNamespace();
			String url = namespace + (namespace.endsWith("/") ? "" : "/")
					+ invocation.getProxy().getActionName();
			response.sendRedirect(request.getContextPath() + url);
			return;
		}
		String finalLocation = conditionalParse(location, invocation);
		doExecute(finalLocation, invocation);
	}

	private static Map<String, String> cache = new ConcurrentHashMap<String, String>(
			256);

	@Override
	protected String conditionalParse(String param, ActionInvocation invocation) {
		String result = invocation.getResultCode();
		String namespace = invocation.getProxy().getNamespace();
		String actionName = invocation.getInvocationContext().getName();
		if (namespace.equals("/"))
			namespace = "";
		String templateName = null;
		String location = null;
		if (StringUtils.isNotBlank(styleHolder.get())) {
			templateName = getTemplateName(namespace, actionName, result, true);
			location = getTemplateLocation(templateName);
		}
		if (location == null) {
			templateName = getTemplateName(namespace, actionName, result, false);
			location = cache.get(templateName);
			if (location == null
					|| AppInfo.getStage() == Stage.DEVELOPMENT) {
				ServletContext context = ServletActionContext
						.getServletContext();
				URL url = null;
				location = getTemplateLocation(templateName);
				if (location == null) {
					if (StringUtils.isNotBlank(styleHolder.get())) {
						location = new StringBuilder().append(ftlLocation)
								.append("/meta/result/").append(result)
								.append(".").append(styleHolder.get())
								.append(".ftl").toString();
						try {
							url = context.getResource(location);
						} catch (MalformedURLException e) {
							e.printStackTrace();
						}
					}
					if (url == null) {
						location = new StringBuilder().append(ftlLocation)
								.append("/meta/result/").append(result)
								.append(".ftl").toString();
						try {
							url = context.getResource(location);
						} catch (MalformedURLException e) {
							e.printStackTrace();
						}
					}
					if (url == null
							&& StringUtils.isNotBlank(styleHolder.get())) {
						location = new StringBuilder().append(ftlClasspath)
								.append("/meta/result/").append(result)
								.append(".").append(styleHolder.get())
								.append(".ftl").toString();
						url = ClassLoaderUtil.getResource(
								location.substring(1), AutoConfigResult.class);
					}
					if (url == null)
						location = new StringBuilder().append(ftlClasspath)
								.append("/meta/result/").append(result)
								.append(".ftl").toString();
				}
				cache.put(templateName, location);
			}
		}
		styleHolder.remove();
		return location;
	}

	public static String getTemplateLocation(String templateName) {
		String location = cache.get(templateName);
		if (location == null || AppInfo.getStage() == Stage.DEVELOPMENT) {
			ServletContext context = ServletActionContext.getServletContext();
			URL url = null;
			location = new StringBuilder().append(ftlLocation)
					.append(templateName).append(".ftl").toString();
			try {
				url = context.getResource(location);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			if (url == null) {
				location = new StringBuilder().append(ftlClasspath)
						.append(templateName).append(".ftl").toString();
				url = ClassLoaderUtil.getResource(location.substring(1),
						AutoConfigResult.class);
			}
			if (url == null)
				location = "";
			cache.put(templateName, location);
		}
		return StringUtils.isEmpty(location) ? null : location;
	}

	private String getTemplateName(String namespace, String actionName,
			String result, boolean withStyle) {
		StringBuilder sb = new StringBuilder();
		sb.append(namespace).append('/').append(actionName);
		if (!result.equals(Action.SUCCESS))
			sb.append('_').append(result);
		if (withStyle && StringUtils.isNotBlank(styleHolder.get()))
			sb.append(".").append(styleHolder.get());
		return sb.toString();
	}
}
