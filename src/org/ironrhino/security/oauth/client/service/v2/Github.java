package org.ironrhino.security.oauth.client.service.v2;

import org.ironrhino.core.util.JsonUtils;
import org.ironrhino.security.oauth.client.model.Profile;
import org.ironrhino.security.oauth.client.service.OAuth2Provider;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "github")
@Getter
@Setter
public class Github extends OAuth2Provider {

	private String logo = "https://a248.e.akamai.net/assets.github.com/images/modules/header/logov7@4x-hover.png";

	private String authorizeUrl = "https://github.com/login/oauth/authorize";

	private String accessTokenEndpoint = "https://github.com/login/oauth/access_token";

	private String scope = "user";

	private String profileUrl = "https://api.github.com/user";

	@Override
	public boolean isUseAuthorizationHeader() {
		return true;
	}

	@Override
	protected Profile getProfileFromContent(String content) throws Exception {
		JsonNode data = JsonUtils.fromJson(content, JsonNode.class);
		String uid = data.get("login").textValue();
		Profile p = new Profile();
		p.setUid(generateUid(uid));
		p.setDisplayName(uid);
		p.setPicture(data.get("avatar_url").textValue());
		return p;
	}

}
