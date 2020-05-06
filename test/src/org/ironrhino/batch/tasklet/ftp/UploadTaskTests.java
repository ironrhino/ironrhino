package org.ironrhino.batch.tasklet.ftp;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;

import org.junit.Test;
import org.springframework.batch.repeat.RepeatStatus;

public class UploadTaskTests extends AbstractFtpTaskTest {

	@Test
	public void test() throws Exception {
		String path = "/test.txt";
		File file = createAndWrite(10);
		UploadTask uploadTask = createTask(UploadTask.class);
		uploadTask.setFile(file);
		uploadTask.setPath(path);
		assertThat(uploadTask.execute(null, null), is(RepeatStatus.FINISHED));

		DeleteTask deleteTask = createTask(DeleteTask.class);
		deleteTask.setPath(path);
		assertThat(deleteTask.execute(null, null), is(RepeatStatus.FINISHED));
	}

}
