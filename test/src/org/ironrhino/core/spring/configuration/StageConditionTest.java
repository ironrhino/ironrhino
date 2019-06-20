
package org.ironrhino.core.spring.configuration;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.ironrhino.core.util.AppInfo.KEY_STAGE;
import static org.ironrhino.core.util.AppInfo.getStage;
import static org.ironrhino.core.util.AppInfo.Stage;

import org.junit.Test;

public class StageConditionTest {

	static {
		System.setProperty(KEY_STAGE, Stage.TEST.name());
	}

	private StageCondition condition = new StageCondition();

	@Test
	public void test() {
		assertThat(condition.matches(getAnnotation(TestBean.class)), is(getStage() == Stage.TEST));
		assertThat(condition.matches(getAnnotation(DevelopmentBean.class)), is(getStage() == Stage.DEVELOPMENT));
		assertThat(condition.matches(getAnnotation(ProductionBean.class)), is(getStage() == Stage.PRODUCTION));
		assertThat(condition.matches(getAnnotation(DevelopmentOrTestBean.class)),
				is(getStage() == Stage.DEVELOPMENT || getStage() == Stage.TEST));
		assertThat(condition.matches(getAnnotation(DevelopmentOrProductionBean.class)),
				is(getStage() == Stage.DEVELOPMENT || getStage() == Stage.PRODUCTION));

	}

	private StageConditional getAnnotation(Class<?> clazz) {
		return clazz.getAnnotation(StageConditional.class);
	}

	@StageConditional(Stage.TEST)
	static class TestBean {
	}

	@StageConditional(Stage.DEVELOPMENT)
	static class DevelopmentBean {
	}

	@StageConditional(Stage.PRODUCTION)
	static class ProductionBean {
	}

	@StageConditional({Stage.DEVELOPMENT, Stage.TEST})
	static class DevelopmentOrTestBean {
	}

	@StageConditional({Stage.DEVELOPMENT, Stage.PRODUCTION})
	static class DevelopmentOrProductionBean {
	}
}