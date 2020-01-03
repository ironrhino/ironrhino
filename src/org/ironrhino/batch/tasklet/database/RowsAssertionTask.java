package org.ironrhino.batch.tasklet.database;

import java.util.Map;

import javax.sql.DataSource;

import org.ironrhino.core.jdbc.SqlVerb;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import lombok.Setter;

@Setter
public class RowsAssertionTask implements Tasklet {

	private DataSource dataSource;

	private String sql;

	private Map<String, Object> parameterValues;

	private int expectedRows;

	@Override
	public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
		NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(dataSource);
		int actualRows = (SqlVerb.parseBySql(sql) == SqlVerb.SELECT)
				? template.queryForObject(sql, parameterValues, int.class)
				: template.update(sql, parameterValues);
		if (actualRows != expectedRows)
			throw new UnexpectedJobExecutionException(
					"Expected rows is " + expectedRows + " but actual rows is " + actualRows);
		return RepeatStatus.FINISHED;
	}

}
