package org.ironrhino.batch.tasklet.file;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.core.io.Resource;

import lombok.Setter;

@Setter
public class LinesVerificationTask implements Tasklet {

	private Resource resource;

	private Pattern extractLinesPattern;

	@Override
	public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
		int expectedLines;
		try (BufferedReader br = createReader()) {
			String firstLine = br.readLine();
			if (firstLine == null)
				throw new UnexpectedJobExecutionException("Unexpected empty file: " + resource);
			String count = firstLine;
			if (extractLinesPattern != null) {
				Matcher m = extractLinesPattern.matcher(firstLine);
				if (!m.find())
					throw new UnexpectedJobExecutionException(
							"First line of " + resource + " doesn't matches " + extractLinesPattern.pattern());
				count = m.group(1);
			}
			expectedLines = Integer.valueOf(count);
		}
		try (BufferedReader br = createReader()) {
			long actualLines = br.lines().count() - 1;
			if (actualLines != expectedLines)
				throw new UnexpectedJobExecutionException(
						"Expected lines is " + expectedLines + " but actual lines is " + actualLines);
		}
		return RepeatStatus.FINISHED;
	}

	private BufferedReader createReader() throws IOException {
		return new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
	}

}
