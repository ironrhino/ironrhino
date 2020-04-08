package org.ironrhino.batch.tasklet.file;

import java.io.File;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import lombok.Setter;

@Setter
public class RenameTask implements Tasklet {

	private File source;

	private File target;

	@Override
	public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
		if (!source.isFile())
			throw new UnexpectedJobExecutionException(String.format("%s is not a file", source.toString()));
		if (target.exists())
			throw new UnexpectedJobExecutionException(String.format("%s already exists", target.toString()));
		if (!source.renameTo(target))
			throw new UnexpectedJobExecutionException(
					String.format("Unable to rename %s to %s", source.toString(), target.toString()));
		return RepeatStatus.FINISHED;
	}

}
