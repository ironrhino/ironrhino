package org.ironrhino.core.struts.result;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts2.ServletActionContext;
import org.apache.struts2.dispatcher.StrutsResultSupport;
import org.ironrhino.core.util.RequestUtils;

import com.opensymphony.xwork2.ActionInvocation;

import lombok.extern.slf4j.Slf4j;

@Slf4j
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
		} else if (!RequestUtils.isSameOrigin(request, finalLocation)) {
			log.warn(
					"{} is not same orgigin, recommend:\nServletActionContext.getResponse().sendRedirect(targetUrl);\nreturn NONE;",
					finalLocation);
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
		}
		response.sendRedirect(finalLocation);
	}

}
