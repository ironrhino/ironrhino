package org.ironrhino.core.seq;

import javax.sql.DataSource;

import org.ironrhino.core.configuration.DataSourceConfiguration;
import org.ironrhino.core.seq.DatabaseSequenceTest.DatabaseSequenceConfiguration;
import org.ironrhino.core.sequence.CyclicSequence.CycleType;
import org.ironrhino.core.sequence.Sequence;
import org.ironrhino.core.sequence.cyclic.DatabaseCyclicSequenceDelegate;
import org.ironrhino.core.sequence.simple.DatabaseSimpleSequenceDelegate;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = DatabaseSequenceConfiguration.class)
public class DatabaseSequenceTest extends SequenceTestBase {

	@Configuration
	@Import(DataSourceConfiguration.class)
	static class DatabaseSequenceConfiguration {

		@Bean
		public Sequence sample1Sequence(DataSource dataSource) {
			return new DatabaseSimpleSequenceDelegate(dataSource);
		}

		@Bean
		public Sequence sample2Sequence(DataSource dataSource) {
			DatabaseCyclicSequenceDelegate cs = new DatabaseCyclicSequenceDelegate(dataSource);
			cs.setCycleType(CycleType.MINUTE);
			return cs;
		}

	}
}
