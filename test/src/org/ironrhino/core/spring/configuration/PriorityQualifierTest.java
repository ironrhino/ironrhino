package org.ironrhino.core.spring.configuration;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.ironrhino.core.spring.configuration.PriorityQualifierTest.TestConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class PriorityQualifierTest {

	@Autowired
	@PriorityQualifier("prioritizedTestBean")
	private TestBean testBean;

	@Autowired
	@PriorityQualifier
	private TestBean prioritizedTestBean;

	private TestBean testBean2;

	private TestBean testBean3;

	@Autowired
	@PriorityQualifier("prioritizedTestBean")
	void setTestBean(TestBean testBean) {
		this.testBean2 = testBean;
	}

	@Autowired
	@PriorityQualifier
	void setPrioritizedTestBean(TestBean prioritizedTestBean) {
		this.testBean3 = prioritizedTestBean;
	}

	@Test
	public void testExplicitFieldInjection() {
		assertThat(testBean.getName(), is("prioritizedTestBean"));
	}

	@Test
	public void testImplicitFieldInjection() {
		assertThat(prioritizedTestBean.getName(), is("prioritizedTestBean"));
	}

	@Test
	public void testExplicitSetterInjection() {
		assertThat(testBean2.getName(), is("prioritizedTestBean"));
	}

	@Test
	public void testImplicitSetterInjection() {
		assertThat(testBean3.getName(), is("prioritizedTestBean"));
	}

	@Configuration
	static class TestConfiguration {

		@Bean
		static PriorityQualifierPostProcessor priorityQualifierPostProcessor() {
			return new PriorityQualifierPostProcessor();
		}

		@Bean
		@Primary
		public TestBean testBean() {
			return new TestBean("testBean");
		}

		@Bean
		public TestBean prioritizedTestBean() {
			return new TestBean("prioritizedTestBean");
		}

	}

	@RequiredArgsConstructor
	static class TestBean {

		@Getter
		private final String name;

	}

}
