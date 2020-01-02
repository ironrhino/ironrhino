package org.ironrhino.batch;

import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.StepRegistry;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.step.NoSuchStepException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DefaultJobStepExecutor implements JobStepExecutor {

	@Autowired
	private ApplicationContext rootApplicationContext;

	@Autowired
	private JobExplorer jobExplorer;

	@Autowired
	private StepRegistry stepRegistry;

	@Override
	public void execute(Long jobExecutionId, Long stepExecutionId, String stepName) throws JobExecutionException {
		StepExecution stepExecution = jobExplorer.getStepExecution(jobExecutionId, stepExecutionId);
		if (stepExecution == null)
			throw new NoSuchStepException("No such StepExecution: " + stepExecutionId);
		String jobName = stepExecution.getJobExecution().getJobInstance().getJobName();
		log.info("Prepare execute step[{}#{}] of job[{}#{}]", stepName, stepExecutionId, jobName, jobExecutionId);
		Step step;
		try {
			step = stepRegistry.getStep(jobName, stepName);
		} catch (NoSuchJobException ex) {
			// for unit test
			step = rootApplicationContext.getBean(stepName, Step.class);
		}
		step.execute(stepExecution);
		log.info("Executed step[{}#{}] of job[{}#{}]", stepName, stepExecutionId, jobName, jobExecutionId);
	}

}
