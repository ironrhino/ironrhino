package org.ironrhino.batch.tasklet.ftp;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

import java.io.File;

import org.junit.Test;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

public class RenameTaskTests extends AbstractFtpTaskTestBase {

	@Test
	public void test() throws Exception {
		String path = "/test/test.txt";
		String path2 = "/test/test2.txt";
		File file = createAndWrite(10);
		UploadTask uploadTask = createTask(UploadTask.class);
		uploadTask.setFile(file);
		uploadTask.setPath(path);
		assertThat(uploadTask.execute(mock(StepContribution.class), mock(ChunkContext.class)), is(RepeatStatus.FINISHED));

		try {
			RenameTask renameTask = createTask(RenameTask.class);
			renameTask.setSource(path);
			renameTask.setTarget(path2);
			assertThat(renameTask.execute(mock(StepContribution.class), mock(ChunkContext.class)), is(RepeatStatus.FINISHED));
		} finally {
			DeleteTask deleteTask = createTask(DeleteTask.class);
			deleteTask.setPath(path2);
			assertThat(deleteTask.execute(mock(StepContribution.class), mock(ChunkContext.class)), is(RepeatStatus.FINISHED));
		}
	}

	@Test(expected = UnexpectedJobExecutionException.class)
	public void testFilesNotInSameDirectory() throws Exception {
		String path = "/test/test.txt";
		String path2 = "/test2/test2.txt";
		File file = createAndWrite(10);
		UploadTask uploadTask = createTask(UploadTask.class);
		uploadTask.setFile(file);
		uploadTask.setPath(path);
		assertThat(uploadTask.execute(mock(StepContribution.class), mock(ChunkContext.class)),
				is(RepeatStatus.FINISHED));

		try {
			RenameTask renameTask = createTask(RenameTask.class);
			renameTask.setSource(path);
			renameTask.setTarget(path2);
			assertThat(renameTask.execute(mock(StepContribution.class), mock(ChunkContext.class)),
					is(RepeatStatus.FINISHED));
		} finally {
			DeleteTask deleteTask = createTask(DeleteTask.class);
			deleteTask.setPath(path);
			assertThat(deleteTask.execute(mock(StepContribution.class), mock(ChunkContext.class)),
					is(RepeatStatus.FINISHED));
		}
	}

	@Test(expected = UnexpectedJobExecutionException.class)
	public void testFileNotExists() throws Exception {
		RenameTask renameTask = createTask(RenameTask.class);
		renameTask.setSource("/test.txt");
		renameTask.setTarget("/test2.txt");
		assertThat(renameTask.execute(mock(StepContribution.class), mock(ChunkContext.class)),
				is(RepeatStatus.FINISHED));
	}

}
