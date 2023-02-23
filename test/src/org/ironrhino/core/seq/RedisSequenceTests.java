package org.ironrhino.core.seq;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDateTime;
import java.time.Period;

import org.ironrhino.core.sequence.CyclicSequence.CycleType;
import org.ironrhino.core.sequence.Sequence;
import org.ironrhino.core.sequence.cyclic.RedisCyclicSequence;
import org.ironrhino.core.sequence.simple.RedisSimpleSequence;
import org.ironrhino.core.spring.configuration.RedisConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = RedisSequenceTests.Config.class)
public class RedisSequenceTests extends SequenceTestBase {

	@Autowired
	private RedisCyclicSequence sample3Sequence;

	@Autowired
	private BoundValueOperations<String, String> sample3SequenceOperations;

	@Test
	public void testCrossCycle() {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime yesterday = now.minus(Period.ofDays(1));
		String currentCycle = sample3Sequence.getCycleType().format(now);
		String lastCycle = sample3Sequence.getCycleType().format(yesterday);
		sample3SequenceOperations.set(lastCycle + "9998");
		assertThat(sample3Sequence.nextStringValue(), is(currentCycle + "0001"));

		sample3SequenceOperations.set(lastCycle + "9999");
		assertThat(sample3Sequence.nextStringValue(), is(currentCycle + "0001"));
	}

	@Test
	public void testOverflowToNextCycle() {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime tomorrow = now.plus(Period.ofDays(1));
		String currentCycle = sample3Sequence.getCycleType().format(now);
		String nextCycle = sample3Sequence.getCycleType().format(tomorrow);
		sample3SequenceOperations.set(currentCycle + "9999");
		assertThat(sample3Sequence.nextStringValue(), is(currentCycle + "10000"));
		assertThat(sample3SequenceOperations.get(), is(nextCycle + "0000"));
		assertThat(sample3Sequence.nextStringValue(), is(currentCycle + "10001"));
		assertThat(sample3SequenceOperations.get(), is(nextCycle + "0001"));
	}

	@Test
	public void testOverflowToInvalidDate() {
		// 20230229 is invalid date
		sample3SequenceOperations.set("202302290001");
		sample3Sequence.nextStringValue();
	}

	@Configuration
	static class Config extends RedisConfiguration {

		@Bean
		Sequence sample1Sequence() {
			return new RedisSimpleSequence();
		}

		@Bean
		Sequence sample2Sequence() {
			RedisCyclicSequence cs = new RedisCyclicSequence();
			cs.setCycleType(CycleType.MINUTE);
			cs.setPaddingLength(7);
			return cs;
		}

		@Bean
		RedisCyclicSequence sample3Sequence() {
			RedisCyclicSequence cs = new RedisCyclicSequence();
			cs.setCycleType(CycleType.DAY);
			cs.setPaddingLength(4);
			return cs;
		}

		@Bean
		BoundValueOperations<String, String> sample3SequenceOperations(StringRedisTemplate redisTemplate,
				RedisCyclicSequence sample3Sequence) {
			return redisTemplate.boundValueOps(RedisCyclicSequence.KEY_SEQUENCE + sample3Sequence.getSequenceName());
		}
	}

}
