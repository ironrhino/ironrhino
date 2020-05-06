package org.ironrhino.batch.tasklet.http;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.http.HttpStatus;
import org.springframework.util.StreamUtils;
import org.springframework.util.unit.DataSize;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Setter
@Slf4j
public class DownloadTask implements Tasklet {

	private URL url;

	private File file;

	@Override
	public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
		if (file.exists())
			throw new UnexpectedJobExecutionException(String.format("%s already exists", file.toString()));
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.connect();
		int code = conn.getResponseCode();
		boolean success = HttpStatus.valueOf(code).is2xxSuccessful();
		File temp = new File(file.getParentFile(), file.getName() + ".tmp");
		int size = 0;
		try (InputStream is = success ? conn.getInputStream() : conn.getErrorStream();
				OutputStream os = new FileOutputStream(temp)) {
			size = StreamUtils.copy(is, os);
			if (success)
				temp.renameTo(file);
		} finally {
			conn.disconnect();
		}
		if (!success) {
			throw new UnexpectedJobExecutionException(String.format("Requested %s received status code %d", url, code));
		}
		log.info("Download {}KB from {} to {}", DataSize.ofBytes(size).toKilobytes(), url, file);
		return RepeatStatus.FINISHED;
	}

}
