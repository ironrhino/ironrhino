package org.ironrhino.batch;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBatchTest
@RunWith(SpringRunner.class)
public abstract class UpdateMessageJobTestBase {

	static final String SQL_CREATE_TABLE = ImportMessageJobTest.SQL_CREATE_TABLE;
	static final String SQL_INSERT = "insert into sample_message(id,title,content,createDate) values (?,?,?,?)";

	static final Date createDate = new Date();
	static long count = 1000L;

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Before
	public void setup() {
		jdbcTemplate.execute(SQL_CREATE_TABLE);

		for (int i = 0; i < count; i++)
			jdbcTemplate.update(SQL_INSERT, i, "title" + i, "content" + i, createDate);
	}

	@Test
	public void test() throws Exception {
		long actualCount = jdbcTemplate.queryForObject("select count(*) from sample_message where createDate=?",
				long.class, createDate);
		assertThat(actualCount, is(count));
		JobExecution jobExecution = jobLauncherTestUtils
				.launchJob(new JobParametersBuilder().addDate("createDate", createDate).toJobParameters());
		assertThat(jobExecution.getExitStatus().getExitCode(), is("COMPLETED"));
		actualCount = jdbcTemplate.queryForObject(
				"select count(*) from sample_message where createDate=? and modifyDate is not null", long.class,
				createDate);
		assertThat(actualCount, is(count));
	}

}