package org.ironrhino.security.oauth.server.service;

import static org.ironrhino.core.metadata.Profiles.CLUSTER;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.ironrhino.core.spring.configuration.ServiceImplementationConditional;
import org.ironrhino.security.oauth.server.model.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component("oauthManager")
@ServiceImplementationConditional(profiles = CLUSTER)
public class HybridOAuthManager extends RedisOAuthManager {

	@Autowired
	private ClientManager clientManager;

	@Override
	public Client findClientById(String clientId) {
		if (StringUtils.isBlank(clientId))
			return null;
		Client c = clientManager.get(clientId);
		return c != null && c.isEnabled() ? c : null;
	}

	@Override
	public List<Client> findClientByOwner(UserDetails owner) {
		DetachedCriteria dc = clientManager.detachedCriteria();
		dc.add(Restrictions.eq("owner", owner));
		dc.addOrder(Order.asc("createDate"));
		return clientManager.findListByCriteria(dc);
	}

}
