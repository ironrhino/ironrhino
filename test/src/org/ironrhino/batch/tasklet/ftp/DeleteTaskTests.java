package org.ironrhino.batch.tasklet.ftp;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;

import org.junit.Test;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.repeat.RepeatStatus;

public class DeleteTaskTests extends AbstractFtpTaskTest {

	@Test
	public void test() throws Exception {
		String path = "/test.txt";
		File file = File.createTempFile("target", ".txt");
		file.deleteOnExit();
		UploadTask uploadTask = createTask(UploadTask.class);
		uploadTask.setFile(file);
		uploadTask.setPath(path);
		assertThat(uploadTask.execute(null, null), is(RepeatStatus.FINISHED));

		DeleteTask task = createTask(DeleteTask.class);
		task.setPath(path);
		assertThat(task.execute(null, null), is(RepeatStatus.FINISHED));
	}

	@Test(expected = UnexpectedJobExecutionException.class)
	public void testFileNotExists() throws Exception {
		DeleteTask task = createTask(DeleteTask.class);
		task.setPath("/notexists.txt");
		assertThat(task.execute(null, null), is(RepeatStatus.FINISHED));
	}

}
