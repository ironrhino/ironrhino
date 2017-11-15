package org.ironrhino.common.action;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;

import org.apache.struts2.ServletActionContext;
import org.ironrhino.core.metadata.AutoConfig;

import com.opensymphony.xwork2.ActionSupport;

@AutoConfig(namespace = DirectTemplateAction.NAMESPACE, actionName = DirectTemplateAction.ACTION_NAME)
public class DirectTemplateAction extends ActionSupport {

	private static final long serialVersionUID = -5865373753328653067L;

	public static final String NAMESPACE = "/";
	public static final String ACTION_NAME = "_direct_template_";

	public String getActionBaseUrl() {
		HttpServletRequest request = ServletActionContext.getRequest();
		String uri = request.getRequestURI();
		if (uri.endsWith("/"))
			uri += "index";
		return uri;
	}

	public String getActionNamespace() {
		HttpServletRequest request = ServletActionContext.getRequest();
		String uri = request.getRequestURI();
		return uri.substring(0, uri.lastIndexOf('/'));
	}

	@Override
	public String execute() {
		return (ServletActionContext.getRequest().getDispatcherType() == DispatcherType.ERROR) ? ERROR
				: "directTemplate";
	}

}
