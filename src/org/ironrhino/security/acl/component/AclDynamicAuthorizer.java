package org.ironrhino.security.acl.component;

import org.ironrhino.core.security.dynauth.DynamicAuthorizer;
import org.ironrhino.security.acl.model.Acl;
import org.ironrhino.security.acl.service.AclManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class AclDynamicAuthorizer implements DynamicAuthorizer {

	@Autowired
	private AclManager aclManager;

	@Override
	public boolean authorize(UserDetails user, String resource) {
		if (user != null)
			for (GrantedAuthority ga : user.getAuthorities()) {
				String role = ga.getAuthority();
				Acl acl = aclManager.findAcl(role, resource);
				if (acl != null && acl.isPermitted())
					return true;
			}
		return false;
	}

}
