package org.ironrhino.batch.tasklet.file;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.input.ReversedLinesFileReader;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import lombok.Setter;

@Setter
public class MoveLastLinesToFirstTask implements Tasklet {

	private Resource file;

	private int lines = 1;

	private Charset encoding = StandardCharsets.UTF_8;

	@Override
	public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
		File target = file.getFile();
		Assert.state(target.isFile(), target.getAbsoluteFile() + " should be a file");

		ReversedLinesFileReader rlfr = null;
		List<String> block = new ArrayList<>();
		try {
			rlfr = new ReversedLinesFileReader(target, encoding);
			for (int i = 0; i < lines; i++) {
				String line = rlfr.readLine();
				if (line == null)
					break;
				block.add(0, line);
			}
		} finally {
			if (rlfr != null)
				rlfr.close();
		}
		Assert.state(block.size() == lines, target.getAbsoluteFile() + " should be at least " + lines + " lines");

		File temp = new File(target.getParentFile(), target.getName() + ".tmp");
		try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(temp), encoding))) {
			for (int i = 0; i < lines; i++) {
				bw.write(block.get(i));
				if (i < lines - 1)
					bw.newLine();
			}
			try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(target), encoding))) {
				LinkedList<String> buffer = new LinkedList<>();
				String line;
				while ((line = br.readLine()) != null) {
					buffer.add(line);
					if (buffer.size() == lines + 1) {
						bw.newLine();
						bw.write(buffer.pop());
					}
				}
			}
		}
		temp.renameTo(target);
		return RepeatStatus.FINISHED;
	}

}
