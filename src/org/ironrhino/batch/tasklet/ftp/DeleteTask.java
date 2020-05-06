package org.ironrhino.batch.tasklet.ftp;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

import lombok.Setter;

@Setter
public class DeleteTask extends AbstractFtpTask {

	private String path;

	@Override
	public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
		execute(ftpClient -> {
			String pathname = getPathname(path, ftpClient);
			ftpClient.changeWorkingDirectory(pathname);
			if (ftpClient.getReplyCode() != 550) {
				ftpClient.changeToParentDirectory();
				boolean b = ftpClient.removeDirectory(pathname);
				if (!b)
					throw new UnexpectedJobExecutionException("Failed to delete directory: " + path);
			} else {
				boolean b = ftpClient.deleteFile(pathname);
				if (!b)
					throw new UnexpectedJobExecutionException("Failed to delete file: " + path);
			}
			return null;
		});
		return RepeatStatus.FINISHED;
	}

}
