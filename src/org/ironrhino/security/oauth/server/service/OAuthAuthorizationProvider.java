package org.ironrhino.security.oauth.server.service;

import org.ironrhino.security.oauth.server.domain.OAuthAuthorization;

public interface OAuthAuthorizationProvider {

	OAuthAuthorization get(String accessToken);

}
