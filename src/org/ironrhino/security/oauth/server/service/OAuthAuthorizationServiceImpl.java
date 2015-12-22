package org.ironrhino.security.oauth.server.service;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.BeanUtils;
import org.ironrhino.security.oauth.server.domain.OAuthAuthorization;
import org.ironrhino.security.oauth.server.model.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OAuthAuthorizationServiceImpl implements OAuthAuthorizationService {

	@Autowired
	private OAuthManager oauthManager;

	@Override
	public OAuthAuthorization get(String accessToken) {
		org.ironrhino.security.oauth.server.model.Authorization auth = oauthManager.retrieve(accessToken);
		if (auth == null)
			return null;
		OAuthAuthorization authorization = new OAuthAuthorization();
		BeanUtils.copyProperties(auth, authorization);
		authorization.setClientId(auth.getClient());
		if (StringUtils.isNotBlank(auth.getClient())) {
			Client client = oauthManager.findClientById(auth.getClient());
			if (client != null && client.isEnabled()) {
				authorization.setClientName(client.getName());
				authorization.setClientOwner(client.getOwner().getUsername());
			}
		}
		return authorization;
	}

}
