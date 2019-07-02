package org.ironrhino.security.oauth.server.service;

import org.ironrhino.core.remoting.Remoting;
import org.ironrhino.security.oauth.server.domain.OAuthAuthorization;

@Remoting
public interface OAuthAuthorizationService {

	OAuthAuthorization get(String accessToken);

}
