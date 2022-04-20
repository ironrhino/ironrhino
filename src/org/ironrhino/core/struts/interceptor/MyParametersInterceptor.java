package org.ironrhino.core.struts.interceptor;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
		List<String> list = Arrays.asList(paramName.split("[.\\[\\]'\"]"));
		if (list.contains("class") || list.contains("Class"))
			return true;
		for (String keyword : KEYWORDS)
			if (paramName.startsWith(keyword + '.') || paramName.startsWith(keyword + '['))
				return true;
		return false;
	}

	@Override
	protected void setParameters(Object action, ValueStack stack, Map<String, Object> parameters) {
		cleanup(parameters); // S2-60
		action = ReflectionUtils.getTargetObject(action); // S2-49
		super.setParameters(action, stack, parameters);
	}

	private void cleanup(Map<String, Object> parameters) {
		List<String> fileParams = parameters.entrySet().stream().filter(entry -> entry.getValue() instanceof File[])
				.map(Map.Entry::getKey).collect(Collectors.toList());
		for (String fileParam : fileParams) {
			List<String> tobeRemoved = parameters.keySet().stream()
					.filter(key -> key.startsWith(fileParam + '.') || key.startsWith(fileParam + '['))
					.collect(Collectors.toList());
			tobeRemoved.forEach(parameters::remove);
		}
	}

}
