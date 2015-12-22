package org.ironrhino.security.oauth.server.service;

import org.ironrhino.core.remoting.Remoting;
import org.ironrhino.security.oauth.server.domain.OAuthAuthorization;

@Remoting
public interface OAuthAuthorizationService {

	public OAuthAuthorization get(String accessToken);

}
