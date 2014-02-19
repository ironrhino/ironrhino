package org.ironrhino.security.oauth.client.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.JsonUtils;
import org.ironrhino.security.model.User;
import org.ironrhino.security.oauth.client.model.OAuth1Token;
import org.ironrhino.security.oauth.client.model.OAuth2Token;
import org.ironrhino.security.oauth.client.model.OAuthToken;
import org.ironrhino.security.oauth.client.service.OAuth1Provider;
import org.ironrhino.security.oauth.client.service.OAuth2Provider;
import org.ironrhino.security.oauth.client.service.OAuthProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OAuthTokenUtils {

	private static Logger logger = LoggerFactory
			.getLogger(OAuthTokenUtils.class);

	public static final String USER_ATTRIBUTE_NAME_OAUTH_TOKENS = "oauth_tokens";

	public static void putTokenIntoUserAttribute(OAuthProvider provider,
			User user, OAuthToken token) {
		Map<String, String> tokens = null;
		String str = user.getAttribute(USER_ATTRIBUTE_NAME_OAUTH_TOKENS);
		if (StringUtils.isNotBlank(str)) {
			try {
				tokens = JsonUtils.fromJson(str,
						JsonUtils.STRING_MAP_TYPE);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
		if (tokens == null)
			tokens = new HashMap<String, String>(2, 1);
		if (token != null)
			tokens.put(provider.getName(), token.getSource());
		else
			tokens.remove(provider.getName());
		user.setAttribute(USER_ATTRIBUTE_NAME_OAUTH_TOKENS,
				JsonUtils.toJson(tokens));
	}

	public static OAuthToken getTokenFromUserAttribute(OAuthProvider provider,
			User user) {
		Map<String, String> tokens = null;
		String str = user.getAttribute(USER_ATTRIBUTE_NAME_OAUTH_TOKENS);
		if (StringUtils.isNotBlank(str)) {
			try {
				tokens = JsonUtils.fromJson(str,
						JsonUtils.STRING_MAP_TYPE);
				if (tokens != null) {
					String tokenString = tokens.get(provider.getName());
					if (StringUtils.isNotBlank(tokenString)) {
						if (provider instanceof OAuth1Provider)
							return new OAuth1Token(tokenString);
						else if (provider instanceof OAuth2Provider)
							return new OAuth2Token(tokenString);
						else
							logger.warn(provider + " not supported yet!");
					}
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
		return null;
	}
}
