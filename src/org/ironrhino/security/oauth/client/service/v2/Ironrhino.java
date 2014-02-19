package org.ironrhino.security.oauth.client.service.v2;

import org.ironrhino.core.util.JsonUtils;
import org.ironrhino.security.model.User;
import org.ironrhino.security.oauth.client.model.Profile;
import org.ironrhino.security.oauth.client.service.OAuth2Provider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Ironrhino extends OAuth2Provider {

	@Value("${ironrhino.logo:http://localhost/assets/images/ironrhino-logo.jpg}")
	private String logo;

	@Value("${ironrhino.authorizeUrl:http://localhost/oauth/oauth2/auth}")
	private String authorizeUrl;

	@Value("${ironrhino.accessTokenEndpoint:http://localhost/oauth/oauth2/token}")
	private String accessTokenEndpoint;

	@Value("${ironrhino.scope:http://localhost/}")
	private String scope;

	@Value("${ironrhino.profileUrl:http://localhost/user/self}")
	private String profileUrl;

	@Override
	public String getLogo() {
		return logo;
	}

	@Override
	public String getAuthorizeUrl() {
		return authorizeUrl;
	}

	@Override
	public String getAccessTokenEndpoint() {
		return accessTokenEndpoint;
	}

	@Override
	public String getScope() {
		return scope;
	}

	@Override
	public String getProfileUrl() {
		return profileUrl;
	}

	@Override
	public boolean isUseAuthorizationHeader() {
		return true;
	}

	@Override
	protected Profile getProfileFromContent(String content) throws Exception {
		User user = JsonUtils.fromJson(content, User.class);
		Profile p = new Profile();
		p.setUid(user.getUsername());
		p.setDisplayName(user.getName());
		return p;
	}

}
