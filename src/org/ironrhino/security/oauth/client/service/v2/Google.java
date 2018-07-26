package org.ironrhino.security.oauth.client.service.v2;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.JsonUtils;
import org.ironrhino.security.oauth.client.model.Profile;
import org.ironrhino.security.oauth.client.service.OAuth2Provider;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "google")
@Getter
@Setter
public class Google extends OAuth2Provider {

	private String logo = "http://www.google.com/images/logos/accounts_logo.gif";

	private String authorizeUrl = "https://accounts.google.com/o/oauth2/auth";

	private String accessTokenEndpoint = "https://accounts.google.com/o/oauth2/token";

	private String scope = "https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/userinfo.profile";

	private String profileUrl = "https://www.googleapis.com/oauth2/v1/userinfo";

	public String getAccessKey() {
		return settingControl.getStringValue("oauth." + getName() + ".accessKey");
	}

	@Override
	public boolean isUseAuthorizationHeader() {
		return true;
	}

	@Override
	protected Profile getProfileFromContent(String content) throws Exception {
		JsonNode data = JsonUtils.fromJson(content, JsonNode.class);
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
	protected String invoke(String protectedURL, Map<String, String> params, Map<String, String> headers)
			throws IOException {
		if (params == null)
			params = new HashMap<>(2, 1);
		if (StringUtils.isNotBlank(getAccessKey()))
			params.put("key", getAccessKey());
		return super.invoke(protectedURL, params, headers);
	}

}
