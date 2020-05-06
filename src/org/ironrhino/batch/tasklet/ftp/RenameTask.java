package org.ironrhino.batch.tasklet.ftp;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

import lombok.Setter;

@Setter
public class RenameTask extends AbstractFtpTask {

	private String source;

	private String target;

	@Override
	public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
		execute(ftpClient -> {
			String _fromPath = getPathname(source, ftpClient);
			String _toPath = getPathname(target, ftpClient);
			String s1 = _fromPath.substring(0, _fromPath.lastIndexOf('/'));
			String s2 = _toPath.substring(0, _toPath.lastIndexOf('/'));
			if (!s1.equals(s2))
				throw new UnexpectedJobExecutionException(
						"source[" + source + "] and target[" + target + "] should in the same directory");
			ftpClient.changeWorkingDirectory(s1);
			boolean b = ftpClient.rename(_fromPath.substring(_fromPath.lastIndexOf('/') + 1),
					_toPath.substring(_toPath.lastIndexOf('/') + 1));
			if (!b)
				throw new UnexpectedJobExecutionException(
						"Failed to rename source[" + source + "] to target[" + target + "]");
			return null;
		});
		return RepeatStatus.FINISHED;
	}

}
