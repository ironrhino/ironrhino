package org.ironrhino.common.action;

import java.util.List;

import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.scheduled.ScheduledTaskCircuitBreaker;
import org.ironrhino.core.scheduled.ScheduledTaskRegistry;
import org.ironrhino.core.scheduled.ScheduledTaskRegistry.ScheduledTask;
import org.ironrhino.core.security.role.UserRole;
import org.ironrhino.core.struts.BaseAction;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.interceptor.annotations.InputConfig;

import lombok.Getter;
import lombok.Setter;

@AutoConfig
@Authorize(ifAnyGranted = UserRole.ROLE_ADMINISTRATOR)
public class ScheduledAction extends BaseAction {

	private static final long serialVersionUID = 8180265410790553918L;

	@Autowired
	private ScheduledTaskRegistry scheduledTaskRegistry;

	@Autowired(required = false)
	@Getter
	private ScheduledTaskCircuitBreaker circuitBreaker;

	@Setter
	private String task;

	@Setter
	private boolean shortCircuit;

	@Getter
	private List<ScheduledTask> tasks;

	@Override
	public String execute() {
		tasks = scheduledTaskRegistry.getTasks();
		return SUCCESS;
	}

	@InputConfig(resultName = SUCCESS)
	public String shortCircuit() {
		if (circuitBreaker != null)
			circuitBreaker.setShortCircuit(task, shortCircuit);
		return execute();
	}

}
