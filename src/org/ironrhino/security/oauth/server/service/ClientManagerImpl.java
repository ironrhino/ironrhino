package org.ironrhino.security.oauth.server.service;

import java.io.Serializable;
import java.util.List;

import org.ironrhino.core.aop.AopContext;
import org.ironrhino.core.cache.CacheNamespaceProvider;
import org.ironrhino.core.cache.CheckCache;
import org.ironrhino.core.cache.EvictCache;
import org.ironrhino.core.service.BaseManagerImpl;
import org.ironrhino.security.oauth.server.model.Client;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.Getter;
import lombok.Setter;

@Service
public class ClientManagerImpl extends BaseManagerImpl<Client> implements ClientManager, CacheNamespaceProvider {

	protected static final String DEFAULT_CACHE_NAMESPACE = "oauth:client";

	@Getter
	@Setter
	private String cacheNamespace = DEFAULT_CACHE_NAMESPACE;

	@Override
	@Transactional(readOnly = true)
	@CheckCache(key = "${id}", timeToIdle = "3600")
	public Client get(Serializable id) {
		return super.get(id);
	}

	@Override
	@Transactional
	@EvictCache(key = "${client.id}")
	public void delete(Client client) {
		super.delete(client);
	}

	@Override
	@Transactional
	@EvictCache(key = "${client.id}")
	public void save(Client client) {
		super.save(client);
	}

	@Override
	@Transactional
	@EvictCache(key = "${client.id}")
	public void update(Client client) {
		super.update(client);
	}

	@Override
	@Transactional
	@EvictCache(key = "${key = [];foreach (client : " + AopContext.CONTEXT_KEY_RETVAL
			+ ") { key.add(client.id);} return key;}")
	public List<Client> delete(Serializable... id) {
		return super.delete(id);
	}

}
