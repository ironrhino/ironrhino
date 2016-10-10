package org.ironrhino.common.support;

import static org.ironrhino.core.metadata.Profiles.DEFAULT;

import org.ironrhino.core.scheduled.ScheduledTaskCircuitBreaker;
import org.ironrhino.core.spring.configuration.ServiceImplementationConditional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@ServiceImplementationConditional(profiles = { DEFAULT })
@Primary
public class SettingScheduledTaskCircuitBreaker implements ScheduledTaskCircuitBreaker {

	private static final String SETTING_KEY_PREFIX_SHORT_CIRCUIT = "ShortCircuit:";

	@Autowired
	private SettingControl settingControl;

	@Override
	public boolean isShortCircuit(String task) {
		return settingControl.getBooleanValue(SETTING_KEY_PREFIX_SHORT_CIRCUIT + task, false);
	}

	@Override
	public void setShortCircuit(String task, boolean value) {
		settingControl.setValue(SETTING_KEY_PREFIX_SHORT_CIRCUIT + task, String.valueOf(value), true, true);
	}

}
