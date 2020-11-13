package org.ironrhino.common.action;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.JsonConfig;
import org.ironrhino.core.metadata.Scope;
import org.ironrhino.core.security.role.UserRole;
import org.ironrhino.core.spring.ApplicationContextConsole;
import org.ironrhino.core.struts.BaseAction;
import org.ironrhino.core.util.JsonUtils;
import org.ironrhino.core.util.ReflectionUtils;
import org.mvel2.CompileException;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.interceptor.annotations.InputConfig;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@AutoConfig
@Authorize(ifAnyGranted = UserRole.ROLE_ADMINISTRATOR)
@Slf4j
public class ConsoleAction extends BaseAction {

	private static final long serialVersionUID = 8180265410790553918L;

	@NotEmpty
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
	@Valid
	@InputConfig(resultName = SUCCESS)
	public String execute() throws Exception {
		log.info("Executing: {}", mask(expression));
		try {
			result = applicationContextConsole.execute(expression, scope);
			addActionMessage(getText("operate.success") + (result != null ? (":" + JsonUtils.toJson(result)) : ""));
			return SUCCESS;
		} catch (Throwable throwable) {
			if (throwable instanceof InvocationTargetException)
				throwable = ((InvocationTargetException) throwable).getTargetException();
			if (throwable.getCause() instanceof InvocationTargetException)
				throwable = ((InvocationTargetException) throwable.getCause()).getTargetException();
			String msg = mask(throwable.getLocalizedMessage());
			try {
				ReflectionUtils.setFieldValue(throwable, "detailMessage", msg);
				if (throwable instanceof CompileException) {
					String expr = mask(new String(((CompileException) throwable).getExpr()));
					ReflectionUtils.setFieldValue(throwable, "expr", expr.toCharArray());
				}
			} catch (Exception e) {
				// ignore;
			}
			log.error(msg, throwable);
			addActionError(getText("error") + (StringUtils.isNotBlank(msg) ? (": " + msg) : ""));
			return ERROR;
		}
	}

	@Valid
	@InputConfig(resultName = SUCCESS)
	@JsonConfig(root = "result")
	public String executeJson() {
		log.info("Executing: {}", mask(expression));
		try {
			result = applicationContextConsole.execute(expression, scope);
		} catch (Throwable throwable) {
			if (throwable instanceof InvocationTargetException)
				throwable = ((InvocationTargetException) throwable).getTargetException();
			String msg = mask(throwable.getLocalizedMessage());
			try {
				ReflectionUtils.setFieldValue(throwable, "detailMessage", msg);
				if (throwable instanceof CompileException) {
					String expr = mask(new String(((CompileException) throwable).getExpr()));
					ReflectionUtils.setFieldValue(throwable, "expr", expr.toCharArray());
				}
			} catch (Exception e) {
				// ignore;
			}
			log.error(msg, throwable);
			addActionError(getText("error") + (StringUtils.isNotBlank(msg) ? (": " + msg) : ""));
			Map<String, Collection<String>> map = new HashMap<>();
			map.put("actionErrors", getActionErrors());
			result = map;
		}
		return JSON;
	}

	public String interactive() {
		return "interactive";
	}

	private static String mask(String message) {
		if (message.contains("setPassword(")) {
			message = message.replaceAll("(setPassword\\(\\s*(['\"])).*(\\2\\s*\\))", "$1" + MASK + "$3");
		} else if (message.contains(".password")) {
			message = message.replaceAll("(.password\\s*=\\s*(['\"])).*(\\2)", "$1" + MASK + "$3");
		}
		return message;
	}

	private static final String MASK = "********";

}
