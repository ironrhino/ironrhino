package org.ironrhino.core.struts.result;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.dispatcher.StrutsResultSupport;
import org.ironrhino.core.util.RequestUtils;

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
		if (finalLocation.indexOf("://") < 0) {
			String url = request.getRequestURL().toString();
			if (finalLocation.startsWith("/")) {
				String ctxPath = request.getContextPath();
				finalLocation = url.substring(0,
						url.indexOf(StringUtils.isBlank(ctxPath) ? "/" : ctxPath, url.indexOf("://") + 3)
								+ ctxPath.length())
						+ finalLocation;
			} else {
				finalLocation = url.substring(0, url.lastIndexOf('/') + 1) + finalLocation;
			}
			finalLocation = response.encodeRedirectURL(finalLocation.toString());
		} else if (!RequestUtils.isSameOrigin(request, finalLocation)) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
		}
		response.sendRedirect(finalLocation);
	}

}
