package org.ironrhino.sample.crud;

import java.io.Serializable;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.ironrhino.core.sequence.Sequence;
import org.ironrhino.core.sequence.cyclic.DatabaseCyclicSequenceDelegate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TaskConfiguration {

	@Bean
	public Sequence taskNo(DataSource dataSource) {
		return new DatabaseCyclicSequenceDelegate(dataSource);
	}

	@Bean
	public TaskNoGenerator taskNoGenerator() {
		return new TaskNoGenerator();
	}

	public class TaskNoGenerator implements IdentifierGenerator {

		@Resource
		private Sequence taskNo;

		@Override
		public Serializable generate(SharedSessionContractImplementor session, Object obj) {
			return taskNo.nextStringValue();
		}

	}

}
