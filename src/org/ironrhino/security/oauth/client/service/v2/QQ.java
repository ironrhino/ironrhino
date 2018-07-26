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
@ConfigurationProperties(prefix = "qq")
@Getter
@Setter
public class QQ extends OAuth2Provider {

	private String logo = "http://qzonestyle.gtimg.cn/qzone/vas/opensns/res/img/Connect_logo_5.png";

	private String authorizeUrl = "https://graph.qq.com/oauth2.0/authorize";

	private String accessTokenEndpoint = "https://graph.qq.com/oauth2.0/token";

	private String scope = "";

	private String profileUrl = "https://graph.qq.com/oauth2.0/me";

	@Override
	public boolean isUseAuthorizationHeader() {
		return false;
	}

	@Override
	protected String getAccessTokenParameterName() {
		return "access_token";
	}

	@Override
	protected Profile getProfileFromContent(String content) throws Exception {
		content = content.substring(content.indexOf('(') + 1, content.lastIndexOf(')'));
		JsonNode data = JsonUtils.fromJson(content, JsonNode.class);
		Profile p = new Profile();
		p.setUid(data.get("openid").textValue());
		return p;
	}

	@Override
	protected void postProcessProfile(Profile p, String accessToken) throws Exception {
		String uid = p.getUid();
		p.setUid(generateUid(uid));
		String content = invoke(accessToken,
				"https://graph.qq.com/user/get_user_info?oauth_consumer_key=" + getClientId() + "&openid=" + uid);
		JsonNode data = JsonUtils.fromJson(content, JsonNode.class);
		p.setDisplayName(data.get("nickname").textValue());
		p.setName(data.get("nickname").textValue());
	}

}
