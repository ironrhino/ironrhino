package org.ironrhino.security.oauth.server.service;

import java.io.Serializable;
import java.util.List;

import org.ironrhino.core.aop.AopContext;
import org.ironrhino.core.cache.CheckCache;
import org.ironrhino.core.cache.EvictCache;
import org.ironrhino.core.service.BaseManagerImpl;
import org.ironrhino.security.oauth.server.model.Client;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClientManagerImpl extends BaseManagerImpl<String, Client> implements ClientManager {

	@Override
	@Transactional(readOnly = true)
	@CheckCache(namespace = "oauth:client", key = "${id}", timeToIdle = "3600")
	public Client get(String id) {
		return super.get(id);
	}

	@Override
	@Transactional
	@EvictCache(namespace = "oauth:client", key = "${client.id}")
	public void delete(Client client) {
		super.delete(client);
	}

	@Override
	@Transactional
	@EvictCache(namespace = "oauth:client", key = "${client.id}")
	public void save(Client client) {
		super.save(client);
	}

	@Override
	@Transactional
	@EvictCache(namespace = "oauth:client", key = "${client.id}")
	public void update(Client client) {
		super.update(client);
	}

	@Override
	@Transactional
	@EvictCache(namespace = "oauth:client", key = "${key = [];foreach (client : " + AopContext.CONTEXT_KEY_RETVAL
			+ ") { key.add(client.id);} return key;}")
	public List<Client> delete(Serializable... id) {
		return super.delete(id);
	}

}
