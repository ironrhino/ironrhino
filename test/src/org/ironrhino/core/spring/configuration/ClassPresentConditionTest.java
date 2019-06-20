package org.ironrhino.core.spring.configuration;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

public class ClassPresentConditionTest {

	private ClassPresentCondition condition = new ClassPresentCondition();

	@Test
	public void test() {
		assertThat(condition.matches(getAnnotation(ClassNotPresentBean.class)), is(false));
		assertThat(condition.matches(getAnnotation(NegatedClassNotPresentBean.class)), is(true));
		assertThat(condition.matches(getAnnotation(ClassPresentBean.class)), is(true));
		assertThat(condition.matches(getAnnotation(NegatedClassPresentBean.class)), is(false));
		assertThat(condition.matches(getAnnotation(PartialClassNotPresentBean.class)), is(false));
	}

	private ClassPresentConditional getAnnotation(Class<?> clazz) {
		return clazz.getAnnotation(ClassPresentConditional.class);
	}

	@ClassPresentConditional(value = {"NotPresent"})
	static class ClassNotPresentBean {

	}

	@ClassPresentConditional(value = "NotPresent", negated = true)
	static class NegatedClassNotPresentBean {

	}

	@ClassPresentConditional(value = "org.ironrhino.core.spring.configuration.ClassPresentConditionTest")
	static class ClassPresentBean {

	}

	@ClassPresentConditional(value = "org.ironrhino.core.spring.configuration.ClassPresentConditionTest", negated = true)
	static class NegatedClassPresentBean {

	}

	@ClassPresentConditional(value = {"org.ironrhino.core.spring.configuration.ClassPresentConditionTest",
			"NotPresent"})
	static class PartialClassNotPresentBean {

	}
}