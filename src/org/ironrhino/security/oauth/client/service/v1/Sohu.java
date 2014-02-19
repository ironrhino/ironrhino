package org.ironrhino.security.oauth.client.service.v1;

import org.ironrhino.core.util.JsonUtils;
import org.ironrhino.security.oauth.client.model.Profile;
import org.ironrhino.security.oauth.client.service.OAuth1Provider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

@Component
public class Sohu extends OAuth1Provider {

	@Value("${sohu.requestTokenUrl:http://api.t.sohu.com/oauth/request_token}")
	private String requestTokenUrl;

	@Value("${sohu.authorizeUrl:http://api.t.sohu.com/oauth/authorize}")
	private String authorizeUrl;

	@Value("${sohu.accessTokenUrl:http://api.t.sohu.com/oauth/access_token}")
	private String accessTokenUrl;

	@Value("${sohu.logo:http://s1.cr.itc.cn/img/t/logo_sp6.png}")
	private String logo;

	@Value("${sohu.profileUrl:http://api.t.sohu.com/account/verify_credentials.json}")
	private String profileUrl;

	@Override
	public String getProfileUrl() {
		return profileUrl;
	}

	@Override
	public String getLogo() {
		return logo;
	}

	@Override
	public String getRequestTokenUrl() {
		return requestTokenUrl;
	}

	@Override
	public String getAuthorizeUrl() {
		return authorizeUrl;
	}

	@Override
	public String getAccessTokenUrl() {
		return accessTokenUrl;
	}

	@Override
	protected Profile getProfileFromContent(String content) throws Exception {
		JsonNode data = JsonUtils.getObjectMapper().readValue(content,
				JsonNode.class);
		String uid = data.get("id").textValue();
		String name = data.get("name").textValue();
		String displayName = data.get("screen_name").textValue();
		Profile p = new Profile();
		p.setUid(generateUid(uid));
		p.setName(name);
		p.setDisplayName(displayName);
		p.setLocation(data.get("location").textValue());
		p.setPicture(data.get("profile_image_url").textValue());
		return p;
	}
}
