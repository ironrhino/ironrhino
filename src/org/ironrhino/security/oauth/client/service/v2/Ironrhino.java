package org.ironrhino.security.oauth.client.service.v2;

import org.ironrhino.core.util.JsonUtils;
import org.ironrhino.security.model.User;
import org.ironrhino.security.oauth.client.model.Profile;
import org.ironrhino.security.oauth.client.service.OAuth2Provider;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "ironrhino")
@Getter
@Setter
public class Ironrhino extends OAuth2Provider {

	private String logo = "http://localhost:8080/assets/images/ironrhino-logo.jpg";

	private String authorizeUrl = "http://localhost:8080/oauth/auth";

	private String accessTokenEndpoint = "http://localhost:8080/oauth/oauth2/token";

	private String scope = "http://localhost:8080/";

	private String profileUrl = "http://localhost:8080/api/user/@self";

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
