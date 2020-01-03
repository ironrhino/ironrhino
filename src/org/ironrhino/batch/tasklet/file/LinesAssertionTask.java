package org.ironrhino.batch.tasklet.file;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.core.io.Resource;

import lombok.Setter;

@Setter
public class LinesAssertionTask implements Tasklet {

	private Resource resource;

	private int expectedLines;

	@Override
	public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
			long actualLines = br.lines().count();
			if (actualLines != expectedLines)
				throw new UnexpectedJobExecutionException(
						"Expected lines is " + expectedLines + " but actual lines is " + actualLines);
		}
		return RepeatStatus.FINISHED;
	}

}
