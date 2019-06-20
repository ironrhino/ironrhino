package org.ironrhino.core.spring.configuration;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

public class ResourcePresentConditionTest {

	private ResourcePresentCondition condition = new ResourcePresentCondition();

	@Test
	public void test() {
		assertThat(condition.matches(getAnnotation(ResourcePresentBean.class)), is(true));
		assertThat(condition.matches(getAnnotation(ResourceNotPresentBean.class)), is(false));
		assertThat(condition.matches(getAnnotation(NegatedResourcePresentBean.class)), is(false));
		assertThat(condition.matches(getAnnotation(NegatedResourceNotPresentBean.class)), is(true));
		assertThat(condition.matches(getAnnotation(PartialResourcesPresentBean.class)), is(false));
	}

	private ResourcePresentConditional getAnnotation(Class<?> clazz) {
		return clazz.getAnnotation(ResourcePresentConditional.class);
	}

	@ResourcePresentConditional(value = "classpath:resources/spring/applicationContext.properties")
	static class ResourcePresentBean {
	}

	@ResourcePresentConditional(value = "classpath:resources/spring/notPresentResource.properties")
	static class ResourceNotPresentBean {
	}

	@ResourcePresentConditional(value = "classpath:resources/spring/applicationContext.properties", negated = true)
	static class NegatedResourcePresentBean {
	}

	@ResourcePresentConditional(value = "classpath:resources/spring/notPresentResource.properties", negated = true)
	static class NegatedResourceNotPresentBean {
	}

	@ResourcePresentConditional(value = {"classpath:resources/spring/applicationContext.properties",
			"classpath:resources/spring/notPresentResource.properties"})
	static class PartialResourcesPresentBean {
	}
}