package org.ironrhino.batch.tasklet.file;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Pattern;

import org.junit.Test;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.core.io.FileSystemResource;

public class LinesVerificationTaskTest {

	@Test(expected = IOException.class)
	public void testIOException() throws Exception {
		File nonexist = new File(System.getProperty("java.io.tmpdir"), "nonexist.txt");
		execute(nonexist, null);
	}

	@Test
	public void test() throws Exception {
		File temp = createAndWrite(10, "10");
		execute(temp, null);
	}

	@Test(expected = UnexpectedJobExecutionException.class)
	public void testWrongLines() throws Exception {
		File temp = createAndWrite(10, "11");
		execute(temp, null);
	}

	@Test(expected = NumberFormatException.class)
	public void testInvalidLines() throws Exception {
		File temp = createAndWrite(10, "abc");
		execute(temp, null);
	}

	@Test
	public void testWithPattern() throws Exception {
		Pattern extractLinesPattern = Pattern.compile("total:(\\d+)");
		File temp = createAndWrite(10, "total:10");
		execute(temp, extractLinesPattern);
	}

	@Test(expected = UnexpectedJobExecutionException.class)
	public void testWrongLinesWithPattern() throws Exception {
		Pattern extractLinesPattern = Pattern.compile("total:(\\d+)");
		File temp = createAndWrite(10, "total:11");
		execute(temp, extractLinesPattern);
	}

	@Test(expected = UnexpectedJobExecutionException.class)
	public void testNoMatches() throws Exception {
		Pattern extractLinesPattern = Pattern.compile("total:(\\d+)");
		File temp = createAndWrite(10, "total:");
		execute(temp, extractLinesPattern);
	}

	private static File createAndWrite(int lines, String header) throws IOException {
		File temp = File.createTempFile("test", ".txt");
		try (FileWriter fw = new FileWriter(temp)) {
			fw.write(header);
			fw.write("\n");
			for (int i = 0; i < lines; i++) {
				fw.write("line" + i);
				if (i < lines - 1)
					fw.write("\n");
			}
		}
		return temp;
	}

	private static void execute(File file, Pattern extractLinesPattern) throws Exception {
		try {
			LinesVerificationTask task = new LinesVerificationTask();
			task.setResource(new FileSystemResource(file));
			task.setExtractLinesPattern(extractLinesPattern);
			assertThat(task.execute(mock(StepContribution.class), mock(ChunkContext.class)), is(RepeatStatus.FINISHED));
		} finally {
			file.delete();
		}
	}

}
