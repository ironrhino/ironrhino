package org.ironrhino.core.struts.interceptor;

import java.util.Arrays;
import java.util.Map;

import org.ironrhino.core.util.ReflectionUtils;

import com.opensymphony.xwork2.interceptor.ParametersInterceptor;
import com.opensymphony.xwork2.util.ValueStack;

public class MyParametersInterceptor extends ParametersInterceptor {

	private static final long serialVersionUID = 6108945048207669330L;

	private static final String[] KEYWORDS = "dojo,struts,session,request,response,application,servletRequest,servletResponse,servletContext,parameters,context,top,_memberAccess"
			.split(",");

	@Override
	protected boolean isExcluded(String paramName) {
		if (isInBlacklist(paramName))
			return true;
		return super.isExcluded(paramName);
	}

	private static boolean isInBlacklist(String paramName) {
		if (Arrays.asList(paramName.split("[.\\[\\]'\"]")).contains("class"))
			return true;
		for (String keyword : KEYWORDS)
			if (paramName.startsWith(keyword + '.') || paramName.startsWith(keyword + '['))
				return true;
		return false;
	}

	@Override
	protected void setParameters(Object action, ValueStack stack, Map<String, Object> parameters) {
		action = ReflectionUtils.getTargetObject(action);
		super.setParameters(action, stack, parameters);
	}

}
