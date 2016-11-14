package org.ironrhino.common.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SettingLocaleProvider implements LocaleProvider {

	public static final String SETTING_KEY_AVAILABLE_LOCALES = "availableLocales";

	@Autowired
	protected SettingControl settingControl;

	@Override
	public String[] getAvailableLocales() {
		return settingControl.getStringArray(SETTING_KEY_AVAILABLE_LOCALES);
	}

}
