package org.ironrhino.security.oauth.server.service;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.BeanUtils;
import org.ironrhino.security.oauth.server.domain.OAuthAuthorization;
import org.ironrhino.security.oauth.server.enums.GrantType;
import org.ironrhino.security.oauth.server.model.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OAuthAuthorizationServiceImpl implements OAuthAuthorizationService {

	@Autowired(required = false)
	private OAuthManager oauthManager;

	@Autowired(required = false)
	private List<OAuthAuthorizationProvider> providers;

	@Override
	public OAuthAuthorization get(String accessToken) {
		OAuthAuthorization authorization = null;
		if (providers != null) {
			for (OAuthAuthorizationProvider provider : providers)
				if ((authorization = provider.get(accessToken)) != null)
					break;
		}
		if (authorization == null && oauthManager != null) {
			org.ironrhino.security.oauth.server.model.Authorization auth = oauthManager.retrieve(accessToken);
			if (auth != null) {
				authorization = new OAuthAuthorization();
				BeanUtils.copyProperties(auth, authorization);
				authorization.setClientId(auth.getClient());
				if (StringUtils.isNotBlank(auth.getClient())) {
					Client client = oauthManager.findClientById(auth.getClient());
					if (client != null && client.isEnabled()) {
						authorization.setClientName(client.getName());
						if (auth.getGrantType() == GrantType.client_credentials)
							authorization.setClientOwner(client.getOwner().getUsername());
					}
				}
			}
		}
		return authorization;
	}

}
