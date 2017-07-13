package org.ironrhino.core.metadata;

import org.ironrhino.core.model.Displayable;
import org.ironrhino.core.util.AppInfo;

public enum VerboseMode implements Displayable {
	LOW, // ignore unnecessary message
	MEDIUM, // alert message and auto dismiss
	HIGH; // alert all message

	@Override
	public String toString() {
		return getDisplayName();
	}

	private static VerboseMode current;

	public static VerboseMode current() {
		if (current == null) {
			try {
				String value = AppInfo.getApplicationContextProperties().getProperty("verboseMode");
				if (value != null)
					current = VerboseMode.valueOf(value);
				else
					current = HIGH;
			} catch (Exception e) {
				e.printStackTrace();
				current = HIGH;
			}
		}
		return current;
	}
}
