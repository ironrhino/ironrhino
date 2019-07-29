package org.ironrhino.common.action;

import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.Scope;
import org.ironrhino.core.security.role.UserRole;
import org.ironrhino.core.spring.ApplicationContextConsole;
import org.ironrhino.core.struts.BaseAction;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.interceptor.annotations.InputConfig;
import com.opensymphony.xwork2.validator.annotations.RequiredStringValidator;
import com.opensymphony.xwork2.validator.annotations.Validations;
import com.opensymphony.xwork2.validator.annotations.ValidatorType;

import io.github.resilience4j.circuitbreaker.CircuitBreaker.State;
import lombok.Getter;
import lombok.Setter;

@AutoConfig
@Authorize(ifAnyGranted = UserRole.ROLE_ADMINISTRATOR)
@Getter
@Setter
public class ThrottleAction extends BaseAction {

	private static final long serialVersionUID = 1792824067929741684L;

	@Autowired
	private ApplicationContextConsole console;

	private String name;

	private State oldState;

	private State newState;

	private Integer oldLimitForPeriod;

	private Integer newLimitForPeriod;

	private Integer oldMaxConcurrentCalls;

	private Integer newMaxConcurrentCalls;

	@Override
	@InputConfig(resultName = SUCCESS)
	@Validations(requiredStrings = {
			@RequiredStringValidator(type = ValidatorType.FIELD, fieldName = "name", trim = true, key = "validation.required") })
	public String execute() throws Exception {
		if (oldState != null && newState != null && newState != oldState) {
			String expression = String.format("circuitBreakerRegistry.transitionState('%s','%s','%s')", name, oldState,
					newState);
			console.execute(expression, Scope.APPLICATION);
		} else if (oldLimitForPeriod != null && newLimitForPeriod != null
				&& !newLimitForPeriod.equals(oldLimitForPeriod)) {
			String expression = String.format("rateLimiterRegistry.changeLimitForPeriod('%s',%d,%d)", name,
					oldLimitForPeriod, newLimitForPeriod);
			console.execute(expression, Scope.APPLICATION);
		} else if (oldMaxConcurrentCalls != null && newMaxConcurrentCalls != null
				&& !newMaxConcurrentCalls.equals(oldMaxConcurrentCalls)) {
			String expression = String.format("bulkheadRegistry.changeMaxConcurrentCalls('%s',%d,%d)", name,
					oldMaxConcurrentCalls, newMaxConcurrentCalls);
			console.execute(expression, Scope.APPLICATION);
		}
		return SUCCESS;
	}

}
