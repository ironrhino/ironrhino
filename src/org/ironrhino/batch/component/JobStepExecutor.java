package org.ironrhino.batch.component;

import java.util.Collection;
import java.util.Map;

import org.ironrhino.core.util.ReflectionUtils;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.support.JobLoader;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class JobStepExecutor {

	@Autowired
	private ApplicationContext rootApplicationContext;

	@Autowired
	private JobLoader jobLoader;

	@Autowired
	private JobExplorer jobExplorer;

	public void execute(Long jobExecutionId, Long stepExecutionId, String stepName)
			throws JobInterruptedException, NoSuchJobException {
		String jobName = jobExplorer.getJobExecution(jobExecutionId).getJobInstance().getJobName();
		Map<ConfigurableApplicationContext, Collection<String>> contextToJobNames = ReflectionUtils
				.getFieldValue(jobLoader, "contextToJobNames");
		Step step = contextToJobNames.entrySet().stream().filter(entry -> entry.getValue().contains(jobName))
				.map(entry -> entry.getKey()).findAny().map(ctx -> ctx.getBean(stepName, Step.class))
				.orElseGet(() -> rootApplicationContext.getBean(stepName, Step.class));
		StepExecution stepExecution = jobExplorer.getStepExecution(jobExecutionId, stepExecutionId);
		step.execute(stepExecution);
	}

}
