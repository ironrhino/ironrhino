package org.ironrhino.common.action;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.scheduled.ScheduledTaskCircuitBreaker;
import org.ironrhino.core.security.role.UserRole;
import org.ironrhino.core.struts.BaseAction;
import org.ironrhino.core.util.AnnotationUtils;
import org.ironrhino.core.util.ReflectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.scheduling.annotation.Scheduled;

import com.opensymphony.xwork2.interceptor.annotations.InputConfig;

@AutoConfig
@Authorize(ifAnyGranted = UserRole.ROLE_ADMINISTRATOR)
public class ScheduledAction extends BaseAction {

	private static final long serialVersionUID = 8180265410790553918L;

	@Autowired
	private ConfigurableListableBeanFactory ctx;

	@Autowired(required = false)
	private ScheduledTaskCircuitBreaker circuitBreaker;

	private String task;

	private boolean shortCircuit;

	private static List<String> tasks;

	public ScheduledTaskCircuitBreaker getCircuitBreaker() {
		return circuitBreaker;
	}

	public void setTask(String task) {
		this.task = task;
	}

	public void setShortCircuit(boolean shortCircuit) {
		this.shortCircuit = shortCircuit;
	}

	public List<String> getTasks() {
		return tasks;
	}

	@Override
	public String execute() {
		if (tasks == null) {
			List<String> temp = new ArrayList<>();
			String[] beanNames = ctx.getBeanDefinitionNames();
			for (String beanName : beanNames) {
				if (ctx.isSingleton(beanName)) {
					BeanDefinition bd = ctx.getBeanDefinition(beanName);
					if (bd.isAbstract())
						continue;
					String beanClassName = bd.getBeanClassName();
					Class<?> clz = null;
					try {
						clz = beanClassName != null ? Class.forName(beanClassName)
								: ReflectionUtils.getTargetObject(ctx.getBean(beanName)).getClass();
					} catch (Exception e) {
						continue;
					}
					Set<Method> methods = AnnotationUtils.getAnnotatedMethods(clz, Scheduled.class);
					for (Method m : methods)
						temp.add(beanName + '.' + m.getName() + "()");
				}
			}
			Collections.sort(temp);
			tasks = temp;
		}
		return SUCCESS;
	}

	@InputConfig(resultName = SUCCESS)
	public String shortCircuit() {
		if (circuitBreaker != null)
			circuitBreaker.setShortCircuit(task, shortCircuit);
		return execute();
	}

}
