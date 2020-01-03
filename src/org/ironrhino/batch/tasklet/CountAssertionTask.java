package org.ironrhino.batch.tasklet;

import java.util.Map;

import javax.sql.DataSource;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import lombok.Setter;

@Setter
public class CountAssertionTask implements Tasklet {

	private DataSource dataSource;

	private String countSql;

	private Map<String, Object> parameterValues;

	private long expectedResult;

	@Override
	public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
		long actualResult = new NamedParameterJdbcTemplate(dataSource).queryForObject(countSql, parameterValues,
				long.class);
		if (actualResult != expectedResult)
			throw new UnexpectedJobExecutionException(
					"Expected result is " + expectedResult + " but actual result is " + actualResult);
		return RepeatStatus.FINISHED;
	}

}
