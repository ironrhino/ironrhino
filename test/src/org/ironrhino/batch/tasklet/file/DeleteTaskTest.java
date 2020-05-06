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

public class DeleteTaskTest {

	@Test
	public void test() throws Exception {
		File file = File.createTempFile("target", ".txt");
		try {
			DeleteTask task = new DeleteTask();
			task.setFile(file);
			assertThat(task.execute(mock(StepContribution.class), mock(ChunkContext.class)), is(RepeatStatus.FINISHED));
		} finally {
			file.delete();
		}
	}

	@Test(expected = UnexpectedJobExecutionException.class)
	public void testFileNotExists() throws Exception {
		File file = File.createTempFile("target", ".txt");
		file.delete();
		DeleteTask task = new DeleteTask();
		task.setFile(file);
		assertThat(task.execute(mock(StepContribution.class), mock(ChunkContext.class)), is(RepeatStatus.FINISHED));
	}

}
