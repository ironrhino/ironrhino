package org.ironrhino.security.oauth.server.service;

import org.ironrhino.security.oauth.server.domain.OAuthAuthorization;

@FunctionalInterface
public interface OAuthAuthorizationProvider {

	OAuthAuthorization get(String accessToken);

}
