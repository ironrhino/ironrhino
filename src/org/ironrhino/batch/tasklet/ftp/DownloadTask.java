package org.ironrhino.batch.tasklet.ftp;

import java.io.File;
import java.io.FileOutputStream;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

import lombok.Setter;

@Setter
public class DownloadTask extends AbstractFtpTask {

	private File file;

	private String path;

	@Override
	public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
		execute(ftpClient -> {
			String pathname = getPathname(path, ftpClient);
			try (FileOutputStream os = new FileOutputStream(file)) {
				boolean b = ftpClient.retrieveFile(pathname, os);
				if (!b)
					throw new UnexpectedJobExecutionException("Failed to download file from path: " + path);
			}
			return null;
		});
		return RepeatStatus.FINISHED;
	}

}
