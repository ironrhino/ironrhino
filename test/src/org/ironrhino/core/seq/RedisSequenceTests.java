package org.ironrhino.core.seq;

import org.ironrhino.core.seq.RedisSequenceTests.RedisSequenceConfiguration;
import org.ironrhino.core.sequence.CyclicSequence.CycleType;
import org.ironrhino.core.sequence.Sequence;
import org.ironrhino.core.sequence.cyclic.RedisCyclicSequence;
import org.ironrhino.core.sequence.simple.RedisSimpleSequence;
import org.ironrhino.core.spring.configuration.RedisConfiguration;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = RedisSequenceConfiguration.class)
public class RedisSequenceTests extends SequenceTestBase {

	@Configuration
	static class RedisSequenceConfiguration extends RedisConfiguration {

		@Bean(autowire = Autowire.BY_NAME)
		public Sequence sample1Sequence() {
			return new RedisSimpleSequence();
		}

		@Bean(autowire = Autowire.BY_NAME)
		public Sequence sample2Sequence() {
			RedisCyclicSequence cs = new RedisCyclicSequence();
			cs.setCycleType(CycleType.MINUTE);
			return cs;
		}
	}

}
