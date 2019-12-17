package org.ironrhino.batch;

import java.util.Collection;
import java.util.Map;

import org.ironrhino.core.util.ReflectionUtils;
import org.springframework.batch.core.JobExecution;
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

import lombok.extern.slf4j.Slf4j;

@Component("jobStepExecutor")
@Slf4j
public class DefaultJobStepExecutor implements JobStepExecutor {

	@Autowired
	private ApplicationContext rootApplicationContext;

	@Autowired
	private JobLoader jobLoader;

	@Autowired
	private JobExplorer jobExplorer;

	@Override
	public void execute(Long jobExecutionId, Long stepExecutionId, String stepName)
			throws JobInterruptedException, NoSuchJobException {
		JobExecution jobExecution = jobExplorer.getJobExecution(jobExecutionId);
		if (jobExecution == null)
			throw new IllegalArgumentException("No such jobExecutionId: " + jobExecutionId);
		String jobName = jobExecution.getJobInstance().getJobName();
		log.info("Prepare execute step[{}#{}] of job[{}#{}]", stepName, stepExecutionId, jobName, jobExecutionId);
		Map<ConfigurableApplicationContext, Collection<String>> contextToJobNames = ReflectionUtils
				.getFieldValue(jobLoader, "contextToJobNames");
		Step step = contextToJobNames.entrySet().stream().filter(entry -> entry.getValue().contains(jobName))
				.map(entry -> entry.getKey()).findAny().map(ctx -> ctx.getBean(stepName, Step.class))
				.orElseGet(() -> rootApplicationContext.getBean(stepName, Step.class));
		StepExecution stepExecution = jobExplorer.getStepExecution(jobExecutionId, stepExecutionId);
		step.execute(stepExecution);
		log.info("Executed step[{}#{}] of job[{}#{}]", stepName, stepExecutionId, jobName, jobExecutionId);
	}

}
