package org.ironrhino.core.model;

import java.util.Collection;

public interface Secured {

	public Collection<String> getRoles();

	public String getRolesAsString();

	public void setRolesAsString(String rolesAsString);
}
