package org.ironrhino.core.scheduled;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.IntervalTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.config.Task;
import org.springframework.scheduling.config.TriggerTask;
import org.springframework.scheduling.support.ScheduledMethodRunnable;
import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.Getter;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class ScheduledTaskRegistry implements SchedulingConfigurer {

	@Getter
	private List<ScheduledTask> tasks = new ArrayList<>();

	@Autowired
	private ConfigurableListableBeanFactory ctx;

	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		tasks.addAll(build(taskRegistrar.getFixedRateTaskList(), ScheduledType.FIXEDRATE));
		tasks.addAll(build(taskRegistrar.getCronTaskList(), ScheduledType.CRON));
		tasks.addAll(build(taskRegistrar.getFixedDelayTaskList(), ScheduledType.FIXEDDELAY));
		tasks.addAll(build(taskRegistrar.getTriggerTaskList(), ScheduledType.TRIGGER));
		tasks.sort((t1, t2) -> t1.getName().compareTo(t2.getName()));
	}

	private List<ScheduledTask> build(final List<? extends Task> tasks, final ScheduledType type) {
		return tasks.stream().map(task -> {
			ScheduledTask scheduledTaskInformation = new ScheduledTask();
			scheduledTaskInformation.setType(type);
			scheduledTaskInformation.setName(getName(task.getRunnable()));
			if (task instanceof IntervalTask) {
				IntervalTask intervalTask = (IntervalTask) task;
				scheduledTaskInformation.setInitialDelay(intervalTask.getInitialDelay());
				scheduledTaskInformation.setInterval(intervalTask.getInterval());
			}
			if (task instanceof CronTask) {
				CronTask cronTask = (CronTask) task;
				scheduledTaskInformation.setExpression(cronTask.getExpression());
			}
			if (task instanceof TriggerTask) {
				TriggerTask triggerTask = (TriggerTask) task;
				scheduledTaskInformation.setTrigger(triggerTask.getTrigger().toString());
			}
			return scheduledTaskInformation;
		}).filter(task -> task.getName() != null).collect(Collectors.toList());
	}

	private String getName(Runnable runnable) {
		if (runnable instanceof ScheduledMethodRunnable) {
			ScheduledMethodRunnable smr = (ScheduledMethodRunnable) runnable;
			Method method = smr.getMethod();
			Object target = smr.getTarget();
			Map<String, ?> beans = ctx.getBeansOfType(target.getClass());
			for (Map.Entry<String, ?> entry : beans.entrySet())
				if (entry.getValue() == target)
					return entry.getKey() + '.' + method.getName() + "()";
		}
		return null;
	}

	@Data
	public class ScheduledTask {
		private ScheduledType type;
		private Long interval;
		private Long initialDelay;
		private String name;
		private String expression;
		private String trigger;

		public String getDescription() {
			switch (type) {
			case CRON:
				return expression;
			case FIXEDRATE:
			case FIXEDDELAY:
				StringBuilder sb = new StringBuilder();
				if (initialDelay != null)
					sb.append(initialDelay).append(" | ");
				sb.append(interval);
				return sb.toString();
			case TRIGGER:
				return trigger;
			default:
				return null;
			}
		}
	}

	public enum ScheduledType {
		CRON, FIXEDRATE, FIXEDDELAY, TRIGGER
	}
}