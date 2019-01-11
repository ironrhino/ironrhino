package org.ironrhino.sample.polling;

import java.util.Date;

import org.ironrhino.core.metadata.Profiles;
import org.ironrhino.core.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile(Profiles.SANDBOX)
public class BalanceQueryArchiveTask {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Value("${archive.offset:7}")
	private int offset = 7;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Transactional
	public void execute() throws Exception {
		Date date = DateUtils.addDays(DateUtils.beginOfDay(new Date()), -offset);
		int insertedRows = jdbcTemplate.update(
				"insert into sample_balance_query_his select * from sample_balance_query where createDate < ?", date);
		if (insertedRows > 0) {
			int deletedRows = jdbcTemplate.update("delete from sample_balance_query where createDate < ?", date);
			if (deletedRows == insertedRows)
				logger.info("Archived {} rows of sample_balance_query", deletedRows);
			else
				throw new RuntimeException(
						"Archive sample_balance_query failed, deleted:" + deletedRows + ", inserted:" + insertedRows);
		} else {
			logger.info("Archived 0 rows of sample_balance_query");
		}
	}

}
