package org.ironrhino.batch.tasklet.file;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.junit.Test;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

public class MergeTaskTest {

	@Test
	public void test() throws Exception {
		int files = 10;
		Resource[] sources = new Resource[files];
		for (int i = 0; i < files; i++)
			sources[i] = new FileSystemResource(createAndWrite(i + 1));
		File target = File.createTempFile("target", "txt");
		try {
			MergeTask task = new MergeTask();
			task.setSources(sources);
			task.setTarget(target);
			assertThat(task.execute(null, null), is(RepeatStatus.FINISHED));
			try (BufferedReader br = new BufferedReader(
					new InputStreamReader(new FileInputStream(target), StandardCharsets.UTF_8))) {
				assertThat(br.lines().count(), is(55L));
			}
		} finally {
			Stream.of(sources).forEach(r -> {
				try {
					r.getFile().delete();
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
			target.delete();
		}
	}

	private static File createAndWrite(int lines) throws IOException {
		File temp = File.createTempFile("test", "txt");
		try (FileWriter fw = new FileWriter(temp)) {
			for (int i = 0; i < lines; i++) {
				fw.write(String.valueOf(i));
				if (i < lines - 1)
					fw.write("\n");
			}
		}
		return temp;
	}

}
