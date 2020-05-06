package org.ironrhino.batch.tasklet.ftp;

import java.io.File;
import java.io.FileInputStream;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

import lombok.Setter;

@Setter
public class UploadTask extends AbstractFtpTask {

	private File file;

	private String path;

	@Override
	public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
		execute(ftpClient -> {
			String pathname = getPathname(path, ftpClient);
			String workingDirectory = ftpClient.printWorkingDirectory();
			workingDirectory = org.ironrhino.core.util.StringUtils.trimTailSlash(workingDirectory);
			String relativePath = pathname.substring(workingDirectory.length() + 1);
			String[] arr = relativePath.split("/");
			if (arr.length > 1) {
				StringBuilder sb = new StringBuilder(workingDirectory);
				for (int i = 0; i < arr.length - 1; i++) {
					sb.append("/").append(arr[i]);
					ftpClient.changeWorkingDirectory(sb.toString());
					if (ftpClient.getReplyCode() == 550) {
						ftpClient.makeDirectory(sb.toString());
					}
				}
			}
			try (FileInputStream ins = new FileInputStream(file)) {
				ftpClient.storeFile(pathname, ins);
			}
			return null;
		});
		return RepeatStatus.FINISHED;
	}

}
