package org.ironrhino.batch;

import org.ironrhino.core.remoting.Remoting;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.StepExecution;

@Remoting
@FunctionalInterface
public interface JobStepExecutor {

	public StepExecution execute(Long jobExecutionId, Long stepExecutionId, String stepName)
			throws JobExecutionException;

}
