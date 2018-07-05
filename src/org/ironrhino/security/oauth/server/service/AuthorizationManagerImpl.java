package org.ironrhino.security.oauth.server.service;

import java.io.Serializable;
import java.util.List;

import org.ironrhino.core.aop.AopContext;
import org.ironrhino.core.cache.CheckCache;
import org.ironrhino.core.cache.EvictCache;
import org.ironrhino.core.service.BaseManagerImpl;
import org.ironrhino.core.spring.configuration.ResourcePresentConditional;
import org.ironrhino.security.oauth.server.model.Authorization;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ResourcePresentConditional(value = "resources/spring/applicationContext-oauth.xml", negated = true)
public class AuthorizationManagerImpl extends BaseManagerImpl<String, Authorization> implements AuthorizationManager {

	@Override
	@Transactional(readOnly = true)
	@CheckCache(namespace = "oauth:authorization", key = "${accessToken}", timeToIdle = "3600")
	public Authorization findByAccessToken(String accessToken) {
		return findByNaturalId(accessToken);
	}

	@Override
	@Transactional
	@EvictCache(namespace = "oauth:authorization", key = "${authorization.accessToken}")
	public void delete(Authorization authorization) {
		super.delete(authorization);
	}

	@Override
	@Transactional
	@EvictCache(namespace = "oauth:authorization", key = "${authorization.accessToken}")
	public void save(Authorization authorization) {
		super.save(authorization);
	}

	@Override
	@Transactional
	@EvictCache(namespace = "oauth:authorization", key = "${authorization.accessToken}")
	public void update(Authorization authorization) {
		super.update(authorization);
	}

	@Override
	@Transactional
	@EvictCache(namespace = "oauth:authorization", key = "${key = [];foreach (authorization : "
			+ AopContext.CONTEXT_KEY_RETVAL + ") { key.add(authorization.accessToken);} return key;}")
	public List<Authorization> delete(Serializable... id) {
		return super.delete(id);
	}

}
