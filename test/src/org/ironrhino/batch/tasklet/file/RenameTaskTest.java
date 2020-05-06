package org.ironrhino.batch.tasklet.file;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

import java.io.File;

import org.junit.Test;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
public class RenameTaskTest {

	@Test(expected = UnexpectedJobExecutionException.class)
	public void testTargetExists() throws Exception {
		File source = File.createTempFile("source", ".txt");
		File target = File.createTempFile("target", ".txt");
		try {
			RenameTask task = new RenameTask();
			task.setSource(source);
			task.setTarget(target);
			assertThat(task.execute(mock(StepContribution.class), mock(ChunkContext.class)), is(RepeatStatus.FINISHED));
		} finally {
			source.delete();
			target.delete();
		}
	}

	@Test(expected = UnexpectedJobExecutionException.class)
	public void testSourceNotExists() throws Exception {
		File source = File.createTempFile("source", ".txt");
		source.delete();
		File target = File.createTempFile("target", ".txt");
		try {
			RenameTask task = new RenameTask();
			task.setSource(source);
			task.setTarget(target);
			assertThat(task.execute(mock(StepContribution.class), mock(ChunkContext.class)), is(RepeatStatus.FINISHED));
		} finally {
			target.delete();
		}
	}

	@Test
	public void test() throws Exception {
		File source = File.createTempFile("source", ".txt");
		File target = File.createTempFile("target", ".txt");
		target.delete();
		try {
			RenameTask task = new RenameTask();
			task.setSource(source);
			task.setTarget(target);
			assertThat(task.execute(mock(StepContribution.class), mock(ChunkContext.class)), is(RepeatStatus.FINISHED));
		} finally {
			source.delete();
		}
	}

}
