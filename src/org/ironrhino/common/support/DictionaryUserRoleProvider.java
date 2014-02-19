package org.ironrhino.common.support;

import java.util.Map;

import org.ironrhino.core.security.role.UserRoleProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DictionaryUserRoleProvider implements UserRoleProvider {

	public static final String DICTIONARY_NAME_CUSTOM_USER_ROLE = "CustomUserRole";

	@Autowired
	private DictionaryControl dictionaryControl;

	@Override
	public Map<String, String> getRoles() {
		return dictionaryControl
				.getItemsAsMap(DICTIONARY_NAME_CUSTOM_USER_ROLE);
	}

}
