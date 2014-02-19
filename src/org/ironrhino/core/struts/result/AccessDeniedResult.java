package org.ironrhino.core.struts.result;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts2.ServletActionContext;
import org.ironrhino.core.spring.security.DefaultLoginUrlAuthenticationEntryPoint;
import org.ironrhino.core.util.ApplicationContextUtils;
import org.ironrhino.core.util.AuthzUtils;

import com.opensymphony.xwork2.ActionInvocation;

public class AccessDeniedResult extends AutoConfigResult {

	private static final long serialVersionUID = 5774314746245962433L;

	static DefaultLoginUrlAuthenticationEntryPoint defaultLoginUrlAuthenticationEntryPoint;

	@Override
	public void execute(ActionInvocation invocation) throws Exception {
		if (defaultLoginUrlAuthenticationEntryPoint == null)
			defaultLoginUrlAuthenticationEntryPoint = ApplicationContextUtils
					.getBean(DefaultLoginUrlAuthenticationEntryPoint.class);
		HttpServletRequest request = ServletActionContext.getRequest();
		HttpServletResponse response = ServletActionContext.getResponse();
		if (AuthzUtils.getUserDetails() == null) {
			if (defaultLoginUrlAuthenticationEntryPoint != null)
				response.sendRedirect(response
						.encodeRedirectURL(defaultLoginUrlAuthenticationEntryPoint
								.buildRedirectUrlToLoginPage(request)));
			else
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		} else {
			String finalLocation = conditionalParse(location, invocation);
			doExecute(finalLocation, invocation);
		}
	}

}
