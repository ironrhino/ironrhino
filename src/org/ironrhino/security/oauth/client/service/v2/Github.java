package org.ironrhino.security.oauth.client.service.v2;

import org.ironrhino.core.util.JsonUtils;
import org.ironrhino.security.oauth.client.model.Profile;
import org.ironrhino.security.oauth.client.service.OAuth2Provider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

@Component
public class Github extends OAuth2Provider {

	@Value("${github.logo:https://a248.e.akamai.net/assets.github.com/images/modules/header/logov7@4x-hover.png}")
	private String logo;

	@Value("${github.authorizeUrl:https://github.com/login/oauth/authorize}")
	private String authorizeUrl;

	@Value("${github.accessTokenEndpoint:https://github.com/login/oauth/access_token}")
	private String accessTokenEndpoint;

	@Value("${github.scope:user}")
	private String scope;

	@Value("${github.profileUrl:https://api.github.com/user}")
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
		JsonNode data = JsonUtils.getObjectMapper().readValue(content,
				JsonNode.class);
		String uid = data.get("login").textValue();
		Profile p = new Profile();
		p.setUid(generateUid(uid));
		p.setDisplayName(uid);
		p.setPicture(data.get("avatar_url").textValue());
		return p;
	}

}
