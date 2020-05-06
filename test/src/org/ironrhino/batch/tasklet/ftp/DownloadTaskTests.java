package org.ironrhino.batch.tasklet.ftp;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

public class DownloadTaskTests extends AbstractFtpTaskTest {

	@Test
	public void test() throws Exception {
		String path = "/test.txt";
		File file = createAndWrite(10);
		UploadTask uploadTask = createTask(UploadTask.class);
		uploadTask.setFile(file);
		uploadTask.setPath(path);
		assertThat(uploadTask.execute(mock(StepContribution.class), mock(ChunkContext.class)), is(RepeatStatus.FINISHED));

		try {
			File newFile = File.createTempFile("target", ".txt");
			newFile.deleteOnExit();
			DownloadTask task = createTask(DownloadTask.class);
			task.setPath(path);
			task.setFile(newFile);
			assertThat(task.execute(mock(StepContribution.class), mock(ChunkContext.class)), is(RepeatStatus.FINISHED));
			try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(newFile)))) {
				List<String> list = br.lines().collect(Collectors.toList());
				assertThat(list.size(), is(10));
				for (int i = 0; i < 10; i++)
					assertThat(list.get(i), is(String.valueOf(i)));
			}
		} finally {
			DeleteTask deleteTask = createTask(DeleteTask.class);
			deleteTask.setPath(path);
			assertThat(deleteTask.execute(null, null), is(RepeatStatus.FINISHED));
		}
	}

	@Test(expected = UnexpectedJobExecutionException.class)
	public void testFileNotExists() throws Exception {
		File file = File.createTempFile("target", ".txt");
		file.deleteOnExit();
		DownloadTask task = createTask(DownloadTask.class);
		task.setPath("/notexists.txt");
		task.setFile(file);
		assertThat(task.execute(mock(StepContribution.class), mock(ChunkContext.class)), is(RepeatStatus.FINISHED));
	}

}
