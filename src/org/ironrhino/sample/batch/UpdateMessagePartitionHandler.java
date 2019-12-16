package org.ironrhino.sample.batch;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import org.ironrhino.batch.component.JobStepExecutor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.beans.factory.annotation.Autowired;

public class UpdateMessagePartitionHandler extends TaskExecutorPartitionHandler {

	@Autowired
	private JobStepExecutor jobStepExecutor;

	@Override
	protected FutureTask<StepExecution> createTask(Step step, StepExecution stepExecution) {
		return new FutureTask<>(new Callable<StepExecution>() {
			@Override
			public StepExecution call() throws Exception {
				execute(step, stepExecution);
				return stepExecution;
			}
		});
	}

	protected void execute(Step step, StepExecution stepExecution) throws Exception {
		// step.execute(stepExecution);
		Long jobExecutionId = stepExecution.getJobExecutionId();
		Long stepExecutionId = stepExecution.getId();
		String stepName = step.getName();
		// jobStepExecutor could be remote proxy
		jobStepExecutor.execute(jobExecutionId, stepExecutionId, stepName);
	}

}