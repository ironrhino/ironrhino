package org.ironrhino.batch;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBatchTest
@RunWith(SpringRunner.class)
@ContextConfiguration(locations = { "ctx.xml", "/resources/batch/importMessage.xml" })
public class ImportMessageJobTest {

	static final String SQL_CREATE_TABLE = "CREATE TABLE IF NOT EXISTS sample_message (id bigint(20) NOT NULL,title varchar(255) NOT NULL,content varchar(4000),createDate datetime(6),modifyDate datetime(6),PRIMARY KEY (id));TRUNCATE TABLE sample_message;";

	static final Date createDate = new Date();
	static long count = 1000L;

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Before
	public void setup() {
		jdbcTemplate.execute(SQL_CREATE_TABLE);
	}

	@Test
	public void test() throws Exception {
		Job job = jobLauncherTestUtils.getJob();
		assertThat(job.getName(), is("importMessageJob"));
		JobExecution jobExecution = jobLauncherTestUtils.launchJob(
				new JobParametersBuilder().addDate("createDate", createDate).addLong("count", count).toJobParameters());
		assertThat(jobExecution.getExitStatus().getExitCode(), is("COMPLETED"));
		Long actualCount = jdbcTemplate.queryForObject("select count(*) from sample_message where createDate=?",
				Long.class, createDate);
		assertThat(actualCount, is(count));
	}

}