package org.ironrhino.core.struts.result;

import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.StrutsConstants;
import org.ironrhino.core.metadata.JsonConfig;
import org.ironrhino.core.util.JsonUtils;
import org.springframework.beans.BeanUtils;

import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.Result;
import com.opensymphony.xwork2.ValidationAware;
import com.opensymphony.xwork2.inject.Inject;

public class JsonResult implements Result {

	private static final long serialVersionUID = 5984356746581381755L;

	@Inject(StrutsConstants.STRUTS_I18N_ENCODING)
	private String encoding = "UTF-8";

	private String generateJson(ActionInvocation invocation) {
		Object action = invocation.getAction();
		Method method = BeanUtils.findDeclaredMethod(action.getClass(),
				invocation.getProxy().getMethod(), new Class[0]);
		JsonConfig jsonConfig = method.getAnnotation(JsonConfig.class);
		if (jsonConfig != null && StringUtils.isNotBlank(jsonConfig.root())) {
			Object value = invocation.getStack().findValue(jsonConfig.root());
			return value != null ? JsonUtils.toJson(value) : "{}";
		}

		boolean hasErrors = false;
		Map<String, Object> map = new HashMap<String, Object>();
		if (action instanceof ValidationAware) {
			ValidationAware validationAwareAction = (ValidationAware) action;
			if (validationAwareAction.hasErrors()) {
				hasErrors = true;
				if (validationAwareAction.hasActionErrors()) {
					map.put("actionErrors",
							validationAwareAction.getActionErrors());
				}
				if (validationAwareAction.hasFieldErrors()) {
					map.put("fieldErrors",
							validationAwareAction.getFieldErrors());
				}
				return JsonUtils.toJson(map);
			}
			// else {
			// map.put("hasErrors", false);
			// }
			if (validationAwareAction.hasActionMessages()) {
				// map.put("hasActionMessages", true);
				map.put("actionMessages",
						validationAwareAction.getActionMessages());
			}
			// else {
			// map.put("hasActionMessages", false);
			// }
		}
		if (!hasErrors) {
			if (jsonConfig == null || jsonConfig.propertyName() == null
					|| jsonConfig.propertyName().length == 0) {
				return JsonUtils.toJson(map);
			}
			String[] propertyNameArray = jsonConfig.propertyName();
			if (propertyNameArray != null && propertyNameArray.length > 0) {
				for (String name : propertyNameArray) {
					Object value = invocation.getStack().findValue(name);
					if (value != null)
						map.put(name, value);
				}
			}
		}
		return JsonUtils.toJson(map);
	}

	@Override
	public void execute(ActionInvocation invocation) throws Exception {
		String jsonp = ServletActionContext.getRequest().getParameter("jsonp");
		if (StringUtils.isBlank(jsonp))
			jsonp = ServletActionContext.getRequest().getParameter("callback");
		String json = generateJson(invocation);
		HttpServletResponse response = ServletActionContext.getResponse();
		if (StringUtils.isNotBlank(jsonp))
			response.setContentType("application/javascript;charset="
					+ encoding);
		else
			response.setContentType("application/json;charset=" + encoding);
		if (!response.containsHeader("Cache-Control")) {
			response.setHeader("Cache-Control", "no-cache");
			response.setHeader("Pragma", "no-cache");
			response.setDateHeader("Expires", 0);
		}
		PrintWriter out = response.getWriter();
		if (StringUtils.isNotBlank(jsonp)) {
			out.print(jsonp);
			out.print('(');
		}
		out.print(json);
		if (StringUtils.isNotBlank(jsonp))
			out.print(')');
		out.flush();
		out.close();
	}
}