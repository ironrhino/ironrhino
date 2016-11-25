package org.ironrhino.core.spring.configuration;

import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.AppInfo.RunLevel;

public class RunLevelCondition extends SimpleCondition<RunLevelConditional> {

	@Override
	public boolean matches(RunLevelConditional annotation) {
		return matches(annotation.value(), annotation.negated());
	}

	public static boolean matches(RunLevel runLevel, boolean negated) {
		boolean matched = AppInfo.getRunLevel().compareTo(runLevel) >= 0;
		return matched && !negated || !matched && negated;
	}

	public static boolean matches(String s, boolean negated) {
		return matches(RunLevel.valueOf(s), negated);
	}

}
