package org.ironrhino.batch.tasklet.file;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.Test;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.core.io.FileSystemResource;

public class LinesAssertionTaskTest {

	@Test(expected = IOException.class)
	public void testIOException() throws Exception {
		File nonexist = new File(System.getProperty("java.io.tmpdir"), "nonexist.txt");
		execute(nonexist, 1);
	}

	@Test(expected = UnexpectedJobExecutionException.class)
	public void testUnexpectedJobExecutionException() throws Exception {
		execute(createAndWrite(10), 9);
	}

	@Test
	public void test() throws Exception {
		execute(createAndWrite(10), 10);
	}

	private static File createAndWrite(int lines) throws IOException {
		File temp = File.createTempFile("test", ".txt");
		try (FileWriter fw = new FileWriter(temp)) {
			for (int i = 0; i < lines; i++) {
				fw.write(String.valueOf(i));
				if (i < lines - 1)
					fw.write("\n");
			}
		}
		return temp;
	}

	private static void execute(File file, int expectedLines) throws Exception {
		try {
			LinesAssertionTask task = new LinesAssertionTask();
			task.setResource(new FileSystemResource(file));
			task.setExpectedLines(expectedLines);
			assertThat(task.execute(mock(StepContribution.class), mock(ChunkContext.class)), is(RepeatStatus.FINISHED));
		} finally {
			file.delete();
		}
	}

}
