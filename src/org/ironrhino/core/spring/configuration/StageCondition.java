package org.ironrhino.core.spring.configuration;

import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.AppInfo.Stage;

public class StageCondition extends SimpleCondition<StageConditional> {

	@Override
	public boolean matches(StageConditional annotation) {
		return matches(annotation.value(), annotation.negated());
	}

	public static boolean matches(Stage stage, boolean negated) {
		boolean matched = AppInfo.getStage() == stage;
		return matched && !negated || !matched && negated;
	}

	public static boolean matches(String s, boolean negated) {
		return matches(Stage.valueOf(s), negated);
	}

}
