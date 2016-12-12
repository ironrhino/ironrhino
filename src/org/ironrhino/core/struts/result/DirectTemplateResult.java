package org.ironrhino.core.struts.result;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts2.ServletActionContext;
import org.ironrhino.core.struts.mapper.DefaultActionMapper;
import org.ironrhino.core.util.RequestUtils;

import com.opensymphony.xwork2.ActionInvocation;

public class DirectTemplateResult extends AutoConfigResult {

	private static final long serialVersionUID = 3152452358832384680L;

	@Override
	protected String conditionalParse(String param, ActionInvocation invocation) {
		HttpServletRequest request = ServletActionContext.getRequest();
		String uri = RequestUtils.getRequestUri(request);
		if (uri.endsWith("/")
				&& request.getAttribute(DefaultActionMapper.REQUEST_ATTRIBUTE_KEY_IMPLICIT_DEFAULT_ACTION) != null)
			uri += '/' + DefaultActionMapper.DEFAULT_ACTION_NAME;
		return getTemplateLocation(uri);
	}

}
