package org.ironrhino.security.oauth.server.service;

import org.ironrhino.core.service.BaseManager;
import org.ironrhino.security.oauth.server.model.Authorization;

public interface AuthorizationManager extends BaseManager<Authorization> {

	public Authorization findByAccessToken(String accessToken);

}
