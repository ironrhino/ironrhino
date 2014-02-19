package org.ironrhino.security.oauth.client.service.v2;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.JsonUtils;
import org.ironrhino.security.oauth.client.model.Profile;
import org.ironrhino.security.oauth.client.service.OAuth2Provider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

@Component
public class Google extends OAuth2Provider {

	@Value("${google.logo:http://www.google.com/images/logos/accounts_logo.gif}")
	private String logo;

	@Value("${google.authorizeUrl:https://accounts.google.com/o/oauth2/auth}")
	private String authorizeUrl;

	@Value("${google.accessTokenEndpoint:https://accounts.google.com/o/oauth2/token}")
	private String accessTokenEndpoint;

	@Value("${google.scope:https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/userinfo.profile}")
	private String scope;

	@Value("${google.profileUrl:https://www.googleapis.com/oauth2/v1/userinfo}")
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

	public String getAccessKey() {
		return settingControl.getStringValue("oauth." + getName()
				+ ".accessKey");
	}

	@Override
	public boolean isUseAuthorizationHeader() {
		return true;
	}

	@Override
	protected Profile getProfileFromContent(String content) throws Exception {
		JsonNode data = JsonUtils.getObjectMapper().readValue(content,
				JsonNode.class);
		Profile p = new Profile();
		p.setUid(generateUid(data.get("id").textValue()));
		p.setDisplayName(data.get("name").textValue());
		p.setName(data.get("name").textValue());
		p.setEmail(data.get("email").textValue());
		p.setGender(data.get("gender").textValue());
		p.setLocale(data.get("locale").textValue());
		p.setLink(data.get("link").textValue());
		p.setPicture(data.get("picture").textValue());
		return p;
	}

	@Override
	protected String invoke(String protectedURL, Map<String, String> params,
			Map<String, String> headers) throws IOException {
		if (params == null)
			params = new HashMap<String, String>(2, 1);
		if (StringUtils.isNotBlank(getAccessKey()))
			params.put("key", getAccessKey());
		return super.invoke(protectedURL, params, headers);
	}

}
