package org.ironrhino.core.struts.result;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts2.ServletActionContext;
import org.apache.struts2.dispatcher.StrutsResultSupport;

import com.opensymphony.xwork2.ActionInvocation;

public class RedirectResult extends StrutsResultSupport {

	private static final long serialVersionUID = -6928786957584819539L;

	public RedirectResult() {
	}

	public RedirectResult(String location) {
		super(location);
	}

	@Override
	public void execute(ActionInvocation invocation) throws Exception {
		super.execute(invocation);
	}

	@Override
	protected void doExecute(String finalLocation, ActionInvocation invocation) throws Exception {
		HttpServletRequest request = ServletActionContext.getRequest();
		HttpServletResponse response = ServletActionContext.getResponse();
		if (finalLocation.indexOf("://") < 0 && !finalLocation.startsWith("//")) {
			if (finalLocation.startsWith("/"))
				finalLocation = request.getContextPath() + finalLocation;
			finalLocation = response.encodeRedirectURL(finalLocation);
		}
		response.sendRedirect(finalLocation);
	}

}
