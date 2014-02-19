package org.ironrhino.security.oauth.client.service.v1;

import org.ironrhino.core.util.JsonUtils;
import org.ironrhino.security.oauth.client.model.Profile;
import org.ironrhino.security.oauth.client.service.OAuth1Provider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

@Component
public class Tencent extends OAuth1Provider {

	@Value("${qq.requestTokenUrl:https://open.t.qq.com/cgi-bin/request_token}")
	private String requestTokenUrl;

	@Value("${qq.authorizeUrl:https://open.t.qq.com/cgi-bin/authorize}")
	private String authorizeUrl;

	@Value("${qq.accessTokenUrl:https://open.t.qq.com/cgi-bin/access_token}")
	private String accessTokenUrl;

	@Value("${qq.logo:http://www.qq.com/images/logo.gif}")
	private String logo;

	@Value("${qq.profileUrl:http://open.t.qq.com/api/user/info?format=json}")
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
	public boolean isUseAuthorizationHeader() {
		return false;
	}

	@Override
	public boolean isIncludeCallbackWhenRequestToken() {
		return true;
	}

	@Override
	protected Profile getProfileFromContent(String content) throws Exception {
		JsonNode data = JsonUtils.getObjectMapper()
				.readValue(content, JsonNode.class).get("data");
		JsonNode node = data.get("uid");
		String uid = null;
		String name = data.get("name").textValue();
		if (node != null)
			uid = node.textValue();
		else
			uid = name;
		String displayName = data.get("nick").textValue();
		Profile p = new Profile();
		p.setUid(generateUid(uid));
		p.setName(displayName);
		p.setDisplayName(displayName);
		p.setLocation(data.get("location").textValue());
		p.setPicture(data.get("head").textValue());
		return p;
	}

}
