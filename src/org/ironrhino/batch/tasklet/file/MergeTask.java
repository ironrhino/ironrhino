package org.ironrhino.batch.tasklet.file;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.core.io.Resource;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Setter
@Slf4j
public class MergeTask implements Tasklet {

	private Resource[] sources;

	private File target;

	@Override
	public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
		if (sources == null || sources.length == 0)
			throw new UnexpectedJobExecutionException("sources should not be empty");
		List<Resource> list = new ArrayList<>(Arrays.asList(sources));
		list.sort(Comparator.comparing(Resource::getFilename));
		File temp = new File(target.getParentFile(), target.getName() + ".tmp");
		int lines = 0;
		try (BufferedWriter bw = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(temp), StandardCharsets.UTF_8))) {
			for (Resource resource : list) {
				try (BufferedReader br = new BufferedReader(
						new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
					String line;
					while ((line = br.readLine()) != null) {
						if (lines++ > 0)
							bw.write("\n");
						bw.write(line);
					}
				}
			}
		}
		target.delete();
		if (!temp.renameTo(target))
			throw new UnexpectedJobExecutionException(
					String.format("Unable to rename %s to %s", temp.toString(), target.toString()));
		log.info("Merge {} files total {} lines to {}", sources.length, lines, target);
		return RepeatStatus.FINISHED;
	}

}
