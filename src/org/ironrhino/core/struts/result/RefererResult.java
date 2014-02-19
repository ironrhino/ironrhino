package org.ironrhino.core.struts.result;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.ironrhino.core.util.RequestUtils;

import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.Result;

public class RefererResult implements Result {

	private static final long serialVersionUID = 6487995341022610712L;

	public static final String INCLUDE_QUERY_STRING = "X-Include-Query-String";

	private boolean includeQueryString = false;

	public void setIncludeQueryString(boolean includeQueryString) {
		this.includeQueryString = includeQueryString;
	}

	@Override
	public void execute(ActionInvocation invocation) throws Exception {
		HttpServletRequest request = ServletActionContext.getRequest();
		HttpServletResponse response = ServletActionContext.getResponse();
		String url = request.getParameter("referer");
		if (StringUtils.isBlank(url))
			url = request.getHeader("Referer");
		if (StringUtils.isBlank(url))
			url = RequestUtils.getBaseUrl(request);
		if (includeQueryString
				|| request.getHeader(INCLUDE_QUERY_STRING) != null)
			url += (url.indexOf('?') > 0 ? "&" : "?")
					+ request.getQueryString();
		response.sendRedirect(response.encodeRedirectURL(url));
	}
}
