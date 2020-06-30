package org.ironrhino.security.oauth.server.enums;

public enum GrantType {
	authorization_code, password, refresh_token, client_credentials, jwt_bearer;

	public static final String JWT_BEARER = "urn:ietf:params:oauth:grant-type:jwt-bearer";
}
