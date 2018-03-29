package org.ironrhino.core.seq;

import javax.sql.DataSource;

import org.ironrhino.core.seq.DatabaseSequenceTest.DatabaseSequenceConfiguration;
import org.ironrhino.core.sequence.CyclicSequence.CycleType;
import org.ironrhino.core.sequence.Sequence;
import org.ironrhino.core.sequence.cyclic.DatabaseCyclicSequenceDelegate;
import org.ironrhino.core.sequence.simple.DatabaseSimpleSequenceDelegate;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.zaxxer.hikari.HikariDataSource;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = DatabaseSequenceConfiguration.class)
public class DatabaseSequenceTest extends SequenceTestBase {

	@Configuration
	static class DatabaseSequenceConfiguration {

		@Bean
		public DataSource dataSource() {
			HikariDataSource ds = new HikariDataSource();
			ds.setJdbcUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
			return ds;
		}

		@Bean(autowire = Autowire.BY_NAME)
		public Sequence sample1Sequence() {
			return new DatabaseSimpleSequenceDelegate();
		}

		@Bean(autowire = Autowire.BY_NAME)
		public Sequence sample2Sequence() {
			DatabaseCyclicSequenceDelegate cs = new DatabaseCyclicSequenceDelegate();
			cs.setCycleType(CycleType.MINUTE);
			return cs;
		}

	}
}
