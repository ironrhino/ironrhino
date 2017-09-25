package org.ironrhino.core.spring.configuration;

import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.AppInfo.Stage;

public class StageCondition extends SimpleCondition<StageConditional> {

	@Override
	public boolean matches(StageConditional annotation) {
		return matches(annotation.value(), annotation.negated());
	}

	public static boolean matches(Stage stage, boolean negated) {
		return matches(new Stage[] { stage }, negated);
	}

	public static boolean matches(String stage, boolean negated) {
		return matches(Stage.valueOf(stage), negated);
	}

	public static boolean matches(Stage[] stages, boolean negated) {
		boolean matched = false;
		for (Stage s : stages) {
			if (AppInfo.getStage() == s) {
				matched = true;
				break;
			}
		}
		return matched && !negated || !matched && negated;
	}

	public static boolean matches(String[] stages, boolean negated) {
		Stage[] s = new Stage[stages.length];
		for (int i = 0; i < stages.length; i++)
			s[i] = Stage.valueOf(stages[i]);
		return matches(s, negated);
	}

}
