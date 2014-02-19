package org.ironrhino.common.support;

import org.ironrhino.core.security.dynauth.DynamicAuthorizer;
import org.ironrhino.core.util.AuthzUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class SettingDynamicAuthorizer implements DynamicAuthorizer {

	@Autowired
	protected SettingControl settingControl;

	@Override
	public boolean authorize(UserDetails user, String resource) {
		String ifAnyGranted = settingControl.getStringValue("resource:"
				+ resource);
		return AuthzUtils.authorize(null, ifAnyGranted, null);
	}

}
