package org.ironrhino.security.oauth.client.service;

import org.springframework.beans.factory.annotation.Autowired;

import org.ironrhino.common.support.SettingControl;
import org.ironrhino.security.oauth.client.model.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractOAuthProvider implements OAuthProvider {

	protected static Logger logger = LoggerFactory
			.getLogger(OAuthProvider.class);

	@Autowired
	protected SettingControl settingControl;

	protected boolean forceDisabled;

	public void setForceDisabled(boolean forceDisabled) {
		this.forceDisabled = forceDisabled;
	}

	@Override
	public String getName() {
		return getClass().getSimpleName().toLowerCase();
	}

	@Override
	public boolean isEnabled() {
		return !forceDisabled
				&& settingControl.getBooleanValue("oauth." + getName()
						+ ".enabled", true);
	}

	public int getDisplayOrder() {
		return settingControl.getIntValue("oauth." + getName()
				+ ".displayOrder", 0);
	}

	protected boolean isUseAuthorizationHeader() {
		return true;
	}

	protected String getAuthorizationHeaderType() {
		return "OAuth";
	}

	protected String generateUid(String uid) {
		return "(" + getName() + ")" + uid;
	}

	public abstract String getProfileUrl();

	protected abstract Profile getProfileFromContent(String content)
			throws Exception;

	@Override
	public String toString() {
		return getName();
	}

	@Override
	public int compareTo(OAuthProvider object) {
		if (!(object instanceof AbstractOAuthProvider))
			return 0;
		AbstractOAuthProvider ap = (AbstractOAuthProvider) object;
		if (this.getDisplayOrder() != ap.getDisplayOrder())
			return this.getDisplayOrder() - ap.getDisplayOrder();
		return this.toString().compareTo(ap.toString());
	}

}
