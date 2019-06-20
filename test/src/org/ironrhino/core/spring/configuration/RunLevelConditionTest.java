package org.ironrhino.core.spring.configuration;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.ironrhino.core.util.AppInfo.KEY_RUNLEVEL;
import static org.ironrhino.core.util.AppInfo.RunLevel;

import org.junit.Test;

public class RunLevelConditionTest {

	static {
		System.setProperty(KEY_RUNLEVEL, RunLevel.NORMAL.name());
	}

	@Test
	public void test() {
		assertThat(RunLevelCondition.matches(RunLevel.LOW, false), is(true));
		assertThat(RunLevelCondition.matches(RunLevel.LOW, true), is(false));
		assertThat(RunLevelCondition.matches(RunLevel.NORMAL, false), is(true));
		assertThat(RunLevelCondition.matches(RunLevel.NORMAL, true), is(false));
		assertThat(RunLevelCondition.matches(RunLevel.HIGH, false), is(false));
		assertThat(RunLevelCondition.matches(RunLevel.HIGH, true), is(true));
	}
}