package org.ironrhino.security.acl.service;

import java.io.Serializable;
import java.util.List;

import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.ironrhino.core.cache.CheckCache;
import org.ironrhino.core.cache.EvictCache;
import org.ironrhino.core.service.BaseManagerImpl;
import org.ironrhino.security.acl.model.Acl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AclManagerImpl extends BaseManagerImpl<Acl> implements AclManager {

	@Override
	@Transactional
	@EvictCache(namespace = "acl", key = "${acl.role+acl.resource}")
	public void delete(Acl acl) {
		super.delete(acl);
	}

	@Override
	@Transactional
	@EvictCache(namespace = "acl", key = "${key = [];foreach (acl : retval) { key.add(acl.role+acl.resource);} return key;}")
	public List<Acl> delete(Serializable... id) {
		return super.delete(id);
	}

	@Override
	@Transactional
	@EvictCache(namespace = "acl", key = "${acl.role+acl.resource}")
	public void save(Acl acl) {
		super.save(acl);
	}

	@Override
	@Transactional(readOnly = true)
	@CheckCache(namespace = "acl", key = "${role+resource}")
	public Acl findAcl(String role, String resource) {
		return findByNaturalId("role", role, "resource", resource);
	}

	@Override
	public List<Acl> findAclsByRole(String role) {
		DetachedCriteria dc = detachedCriteria();
		dc.add(Restrictions.eq("role", role));
		return findListByCriteria(dc);
	}

}
