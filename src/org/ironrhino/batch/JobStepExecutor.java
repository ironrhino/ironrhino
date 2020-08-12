package org.ironrhino.batch;

import org.ironrhino.core.remoting.Remoting;
import org.springframework.batch.core.JobExecutionException;

@Remoting
@FunctionalInterface
public interface JobStepExecutor {

	public void execute(Long jobExecutionId, Long stepExecutionId, String stepName) throws JobExecutionException;

}
