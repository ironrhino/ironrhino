package org.ironrhino.batch.tasklet.file;

import java.io.File;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import lombok.Setter;

@Setter
public class DeleteTask implements Tasklet {

	private File file;

	@Override
	public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
		if (!file.isFile())
			throw new UnexpectedJobExecutionException(String.format("%s is not a file", file.toString()));
		if (!file.delete())
			throw new UnexpectedJobExecutionException(String.format("Unable to delete %s", file.toString()));
		return RepeatStatus.FINISHED;
	}

}
