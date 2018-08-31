package org.ironrhino.core.spring.configuration;

import static org.junit.Assert.assertEquals;

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
		assertEquals("prioritizedTestBean", testBean.getName());
	}

	@Test
	public void testImplicitFieldInjection() {
		assertEquals("prioritizedTestBean", prioritizedTestBean.getName());
	}

	@Test
	public void testExplicitSetterInjection() {
		assertEquals("prioritizedTestBean", testBean2.getName());
	}

	@Test
	public void testImplicitSetterInjection() {
		assertEquals("prioritizedTestBean", testBean3.getName());
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
