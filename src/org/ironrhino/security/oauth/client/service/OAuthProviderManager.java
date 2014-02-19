package org.ironrhino.security.oauth.client.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OAuthProviderManager {

	@Autowired(required = false)
	private List<OAuthProvider> providers;

	public List<OAuthProvider> getProviders() {
		if (providers == null)
			return Collections.emptyList();
		List<OAuthProvider> list = new ArrayList<OAuthProvider>(
				providers.size());
		for (OAuthProvider p : providers)
			if (p.isEnabled())
				list.add(p);
		Collections.sort(list);
		return list;
	}

	public OAuthProvider lookup(String id) {
		if (providers == null || StringUtils.isBlank(id))
			return null;
		for (OAuthProvider p : providers)
			if (p.isEnabled() && id.equals(p.getName()))
				return p;
		return null;
	}

}
