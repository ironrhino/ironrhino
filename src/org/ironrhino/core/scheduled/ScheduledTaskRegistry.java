package org.ironrhino.core.scheduled;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.FixedDelayTask;
import org.springframework.scheduling.config.FixedRateTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.scheduling.config.TriggerTask;
import org.springframework.scheduling.support.ScheduledMethodRunnable;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
public class ScheduledTaskRegistry {

	private List<ScheduledTask> tasks = null;

	@Autowired(required = false)
	private List<ScheduledTaskHolder> scheduledTaskHolders = Collections.emptyList();

	@Autowired
	private ApplicationContext ctx;

	public List<ScheduledTask> getTasks() {
		List<ScheduledTask> temp = tasks;
		if (temp == null) {
			synchronized (this) {
				temp = tasks;
				if (temp == null) {
					temp = doGetTasks();
					tasks = temp;
				}
			}
		}
		return tasks;
	}

	private List<ScheduledTask> doGetTasks() {
		List<ScheduledTask> list = new ArrayList<>();
		for (ScheduledTaskHolder sth : scheduledTaskHolders) {
			sth.getScheduledTasks().stream().map(st -> st.getTask()).forEach(task -> {
				ScheduledTask st = new ScheduledTask();
				st.setName(getName(task.getRunnable()));
				if (st.getName() == null)
					return;
				if (task instanceof FixedRateTask) {
					st.setType(ScheduledType.FIXEDRATE);
					FixedRateTask fixedRateTask = (FixedRateTask) task;
					st.setInitialDelay(fixedRateTask.getInitialDelay());
					st.setInterval(fixedRateTask.getInterval());
				} else if (task instanceof FixedDelayTask) {
					st.setType(ScheduledType.FIXEDDELAY);
					FixedDelayTask fixedDelayTask = (FixedDelayTask) task;
					st.setInitialDelay(fixedDelayTask.getInitialDelay());
					st.setInterval(fixedDelayTask.getInterval());
				} else if (task instanceof CronTask) {
					st.setType(ScheduledType.CRON);
					CronTask cronTask = (CronTask) task;
					st.setCron(cronTask.getExpression());
				} else if (task instanceof TriggerTask) {
					st.setType(ScheduledType.TRIGGER);
					TriggerTask triggerTask = (TriggerTask) task;
					st.setTrigger(triggerTask.getTrigger().toString());
				}
				list.add(st);
			});
		}
		list.sort(Comparator.comparing(ScheduledTask::getName));
		return list;
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
		private String cron;
		private String trigger;

		public String getDescription() {
			switch (type) {
			case CRON:
				return cron;
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