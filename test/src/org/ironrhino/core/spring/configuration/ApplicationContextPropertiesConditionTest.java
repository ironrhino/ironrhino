package org.ironrhino.core.spring.configuration;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.ironrhino.core.util.AppInfo.KEY_STAGE;
import static org.ironrhino.core.util.AppInfo.Stage;
import static org.ironrhino.core.util.AppInfo.getStage;

import org.junit.Test;

public class ApplicationContextPropertiesConditionTest {

	static {
		System.setProperty(KEY_STAGE, Stage.TEST.name());
	}

	private ApplicationContextPropertiesCondition condition = new ApplicationContextPropertiesCondition();

	@Test
	public void test() {
		assertThat(condition.matches(getAnnotation(TestBean.class)), is(true));
		assertThat(condition.matches(getAnnotation(TestBeanWithNonexistentKey.class)), is(false));
		assertThat(condition.matches(getAnnotation(TestBeanWithNonexistentValue.class)), is(false));
		assertThat(condition.matches(getAnnotation(TestBeanWithProductionStage.class)),
				is(getStage() == Stage.PRODUCTION));
		assertThat(condition.matches(getAnnotation(TestBeanWithTestStage.class)), is(getStage() == Stage.TEST));
		assertThat(condition.matches(getAnnotation(TestBeanWithDevelopmentStage.class)),
				is(getStage() == Stage.DEVELOPMENT));
	}

	private ApplicationContextPropertiesConditional getAnnotation(Class<?> clazz) {
		return clazz.getAnnotation(ApplicationContextPropertiesConditional.class);
	}

	@ApplicationContextPropertiesConditional(key = "applicationContextPropertiesConditionTest.key", value = "value")
	private class TestBean {
	}

	@ApplicationContextPropertiesConditional(key = "nonexistentKey", value = "value")
	private class TestBeanWithNonexistentKey {
	}

	@ApplicationContextPropertiesConditional(key = "applicationContextPropertiesConditionTest.key", value = "nonexistentValue")
	private class TestBeanWithNonexistentValue {
	}

	@ApplicationContextPropertiesConditional(key = "applicationContextPropertiesConditionTest.stage", value = "PRODUCTION")
	private class TestBeanWithProductionStage {
	}

	@ApplicationContextPropertiesConditional(key = "applicationContextPropertiesConditionTest.stage", value = "TEST")
	private class TestBeanWithTestStage {
	}

	@ApplicationContextPropertiesConditional(key = "applicationContextPropertiesConditionTest.stage", value = "DEVELOPMENT")
	private class TestBeanWithDevelopmentStage {
	}
}