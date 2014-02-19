package org.ironrhino.security.oauth.client.service.v1;

import org.ironrhino.core.util.JsonUtils;
import org.ironrhino.security.oauth.client.model.Profile;
import org.ironrhino.security.oauth.client.service.OAuth1Provider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

@Component
public class Netease extends OAuth1Provider {

	@Value("${netease.requestTokenUrl:http://api.t.163.com/oauth/request_token}")
	private String requestTokenUrl;

	@Value("${netease.authorizeUrl:http://api.t.163.com/oauth/authenticate}")
	private String authorizeUrl;

	@Value("${netease.accessTokenUrl:http://api.t.163.com/oauth/access_token}")
	private String accessTokenUrl;

	@Value("${netease.logo:http://img3.cache.netease.com/t/img10/index/logo.png}")
	private String logo;

	@Value("${netease.profileUrl:http://api.t.163.com/account/verify_credentials.json}")
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
