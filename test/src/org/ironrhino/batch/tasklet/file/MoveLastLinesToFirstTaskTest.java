package org.ironrhino.batch.tasklet.file;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.springframework.batch.repeat.RepeatStatus;

public class MoveLastLinesToFirstTaskTest {

	@Test(expected = IOException.class)
	public void testIOException() throws Exception {
		File nonexist = new File(System.getProperty("java.io.tmpdir"), "nonexist.txt");
		execute(nonexist, -1, 1);
	}

	@Test(expected = IllegalStateException.class)
	public void testIllegalStateException() throws Exception {
		execute(createAndWrite(10), 10, 22);
	}

	@Test
	public void test() throws Exception {
		execute(createAndWrite(10), 10, 2);
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

	private static void execute(File file, int totalLines, int movedLines) throws Exception {
		try {
			MoveLastLinesToFirstTask task = new MoveLastLinesToFirstTask();
			task.setFile(file);
			task.setLines(movedLines);
			assertThat(task.execute(null, null), is(RepeatStatus.FINISHED));
			try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
				List<String> list = br.lines().collect(Collectors.toList());
				assertThat(list.size(), is(totalLines));
				for (int i = 0; i < movedLines; i++) {
					assertThat(list.get(i), is(String.valueOf(totalLines - movedLines + i)));
				}
				for (int i = movedLines; i < totalLines; i++) {
					assertThat(list.get(i), is(String.valueOf(i - movedLines)));
				}
			}
		} finally {
			file.delete();
		}
	}

}
