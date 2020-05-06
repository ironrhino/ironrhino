package org.ironrhino.batch.tasklet.database;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.Test;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

import com.zaxxer.hikari.HikariDataSource;

public class RowsAssertionTaskTest {

	@Test(expected = UnexpectedJobExecutionException.class)
	public void testUnexpectedJobExecutionException() throws Exception {
		try (HikariDataSource ds = create(10)) {
			execute(ds, "select count(*) from test", null, 0);
		}
	}

	@Test
	public void testCount() throws Exception {
		try (HikariDataSource ds = create(10)) {
			execute(ds, "select count(*) from test", null, 10);
			execute(ds, "select count(*) from test where value%2=0", null, 5);
			execute(ds, "select count(*) from test where value%2=1", null, 5);
		}
	}

	@Test
	public void testUpdate() throws Exception {
		try (HikariDataSource ds = create(10)) {
			execute(ds, "update test set value=value+1 where value%2=0", null, 5);
			execute(ds, "select count(*) from test where value%2=1", null, 10);
		}
	}

	private static HikariDataSource create(int rows) throws SQLException {
		HikariDataSource ds = new HikariDataSource();
		ds.setJdbcUrl("jdbc:h2:mem:test;");
		try (Connection conn = ds.getConnection(); Statement stmt = conn.createStatement()) {
			stmt.execute("drop table if exists test");
			stmt.execute("create table if not exists test(value int)");
			for (int i = 0; i < rows; i++) {
				stmt.executeUpdate("insert into test values(" + i + ")");
			}
		}
		return ds;
	}

	private static void execute(DataSource ds, String sql, Map<String, Object> parameterValues, int expectedRows)
			throws Exception {
		RowsAssertionTask task = new RowsAssertionTask();
		task.setDataSource(ds);
		task.setSql(sql);
		task.setParameterValues(parameterValues);
		task.setExpectedRows(expectedRows);
		assertThat(task.execute(mock(StepContribution.class), mock(ChunkContext.class)), is(RepeatStatus.FINISHED));
	}

}
