package org.ironrhino.common.action;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.JsonConfig;
import org.ironrhino.core.metadata.Scope;
import org.ironrhino.core.security.role.UserRole;
import org.ironrhino.core.spring.ApplicationContextConsole;
import org.ironrhino.core.struts.BaseAction;
import org.ironrhino.core.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.interceptor.annotations.InputConfig;
import com.opensymphony.xwork2.validator.annotations.RequiredStringValidator;
import com.opensymphony.xwork2.validator.annotations.Validations;
import com.opensymphony.xwork2.validator.annotations.ValidatorType;

import lombok.Getter;
import lombok.Setter;

@AutoConfig
@Authorize(ifAnyGranted = UserRole.ROLE_ADMINISTRATOR)
public class ConsoleAction extends BaseAction {

	private static final long serialVersionUID = 8180265410790553918L;

	private static Logger logger = LoggerFactory.getLogger(ConsoleAction.class);

	@Getter
	@Setter
	private String expression;

	@Getter
	@Setter
	private Scope scope = Scope.LOCAL;

	@Getter
	private Object result;

	@Autowired
	private ApplicationContextConsole applicationContextConsole;

	@Override
	@InputConfig(resultName = "success")
	@Validations(requiredStrings = {
			@RequiredStringValidator(type = ValidatorType.FIELD, fieldName = "expression", trim = true, key = "validation.required") })
	public String execute() throws Exception {
		try {
			result = applicationContextConsole.execute(expression, scope);
			addActionMessage(getText("operate.success") + (result != null ? (":" + JsonUtils.toJson(result)) : ""));
			return SUCCESS;
		} catch (Throwable throwable) {
			if (throwable instanceof InvocationTargetException)
				throwable = ((InvocationTargetException) throwable).getTargetException();
			if (throwable.getCause() instanceof InvocationTargetException)
				throwable = ((InvocationTargetException) throwable.getCause()).getTargetException();
			logger.error(throwable.getMessage(), throwable);
			String msg = throwable.getLocalizedMessage();
			addActionError(getText("error") + (StringUtils.isNotBlank(msg) ? (": " + msg) : ""));
			Map<String, Collection<String>> map = new HashMap<>();
			map.put("actionErrors", getActionErrors());
			return ERROR;

		}
	}

	@InputConfig(resultName = "success")
	@Validations(requiredStrings = {
			@RequiredStringValidator(type = ValidatorType.FIELD, fieldName = "expression", trim = true, key = "validation.required") })
	@JsonConfig(root = "result")
	public String executeJson() {
		try {
			result = applicationContextConsole.execute(expression, scope);
		} catch (Throwable throwable) {
			if (throwable instanceof InvocationTargetException)
				throwable = ((InvocationTargetException) throwable).getTargetException();
			logger.error(throwable.getMessage(), throwable);
			String msg = throwable.getMessage();
			addActionError(getText("error") + (StringUtils.isNotBlank(msg) ? (": " + throwable.getMessage()) : ""));
			Map<String, Collection<String>> map = new HashMap<>();
			map.put("actionErrors", getActionErrors());
			result = map;
		}
		return JSON;
	}
}
