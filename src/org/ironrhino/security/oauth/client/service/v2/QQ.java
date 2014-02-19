package org.ironrhino.security.oauth.client.service.v2;

import org.ironrhino.core.util.JsonUtils;
import org.ironrhino.security.oauth.client.model.Profile;
import org.ironrhino.security.oauth.client.service.OAuth2Provider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

@Component
public class QQ extends OAuth2Provider {

	@Value("${qq.logo:http://qzonestyle.gtimg.cn/qzone/vas/opensns/res/img/Connect_logo_5.png}")
	private String logo;

	@Value("${qq.authorizeUrl:https://graph.qq.com/oauth2.0/authorize}")
	private String authorizeUrl;

	@Value("${qq.accessTokenEndpoint:https://graph.qq.com/oauth2.0/token}")
	private String accessTokenEndpoint;

	@Value("${qq.scope:}")
	private String scope;

	@Value("${qq.profileUrl:https://graph.qq.com/oauth2.0/me}")
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
		return false;
	}

	@Override
	protected String getAccessTokenParameterName() {
		return "access_token";
	}

	@Override
	protected Profile getProfileFromContent(String content) throws Exception {
		content = content.substring(content.indexOf('(') + 1,
				content.lastIndexOf(')'));
		JsonNode data = JsonUtils.getObjectMapper().readValue(content,
				JsonNode.class);
		Profile p = new Profile();
		p.setUid(data.get("openid").textValue());
		return p;
	}

	@Override
	protected void postProcessProfile(Profile p, String accessToken)
			throws Exception {
		String uid = p.getUid();
		p.setUid(generateUid(uid));
		String content = invoke(accessToken,
				"https://graph.qq.com/user/get_user_info?oauth_consumer_key="
						+ getClientId() + "&openid=" + uid);
		JsonNode data = JsonUtils.getObjectMapper().readValue(content,
				JsonNode.class);
		p.setDisplayName(data.get("nickname").textValue());
		p.setName(data.get("nickname").textValue());
	}

}
