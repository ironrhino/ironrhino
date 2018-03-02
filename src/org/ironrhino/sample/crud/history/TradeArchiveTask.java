package org.ironrhino.sample.crud.history;

import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class TradeArchiveTask {

	@Value("${archive.offset:7}")
	private int offset = 7;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Transactional
	public void execute(Date workdate) throws Exception {
		Date date = DateUtils.addDays(workdate, -offset);
		int insertedRows = jdbcTemplate.update(
				"insert into sample_historical_trade select * from sample_current_trade where workdate < ?", date);
		int deletedRows = jdbcTemplate.update("delete from sample_current_trade where workdate < ?", date);
		if (deletedRows != insertedRows)
			throw new IllegalStateException(
					"Archive failed: insertedRows=" + insertedRows + ", deletedRows=" + deletedRows);
	}

}
