package org.ironrhino.batch.tasklet.ftp;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

import java.io.File;

import org.junit.Test;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

public class UploadTaskTests extends AbstractFtpTaskTest {

	@Test
	public void test() throws Exception {
		String path = "/test.txt";
		File file = createAndWrite(10);
		UploadTask uploadTask = createTask(UploadTask.class);
		uploadTask.setFile(file);
		uploadTask.setPath(path);
		assertThat(uploadTask.execute(mock(StepContribution.class), mock(ChunkContext.class)),
				is(RepeatStatus.FINISHED));

		DeleteTask deleteTask = createTask(DeleteTask.class);
		deleteTask.setPath(path);
		assertThat(deleteTask.execute(mock(StepContribution.class), mock(ChunkContext.class)),
				is(RepeatStatus.FINISHED));
	}

}
