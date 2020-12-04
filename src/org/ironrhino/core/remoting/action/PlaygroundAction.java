package org.ironrhino.core.remoting.action;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.remoting.playground.ServicePlayground;
import org.ironrhino.core.security.role.UserRole;
import org.ironrhino.core.spring.configuration.StageConditional;
import org.ironrhino.core.struts.BaseAction;
import org.ironrhino.core.util.AppInfo.Stage;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.interceptor.annotations.InputConfig;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@AutoConfig
@Authorize(ifAnyGranted = UserRole.ROLE_ADMINISTRATOR)
@StageConditional(value = Stage.PRODUCTION, negated = true)
@Slf4j
public class PlaygroundAction extends BaseAction {

	private static final long serialVersionUID = 8180265410790553918L;

	@Autowired
	@Getter
	private ServicePlayground servicePlayground;

	@Getter
	@Setter
	private Map<String, String> params = new HashMap<>();

	@Getter
	@Setter
	private String method;

	@Override
	@InputConfig(resultName = SUCCESS)
	public String execute() {
		String service = getUid();
		try {
			String result = servicePlayground.invoke(service, method, params);
			addActionMessage(result);
			return SUCCESS;
		} catch (Throwable throwable) {
			if (throwable instanceof InvocationTargetException)
				throwable = ((InvocationTargetException) throwable).getTargetException();
			if (throwable.getCause() instanceof InvocationTargetException)
				throwable = ((InvocationTargetException) throwable.getCause()).getTargetException();
			log.error(throwable.getMessage(), throwable);
			String msg = throwable.getLocalizedMessage();
			addActionError(throwable.getClass().getName() + (StringUtils.isNotBlank(msg) ? (": " + msg) : ""));
			return ERROR;
		}
	}

}
