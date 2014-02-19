package org.ironrhino.core.model;

import java.util.List;

public interface Attributable {

	public List<Attribute> getAttributes();

	public void setAttributes(List<Attribute> attributes);

	public String getAttributesAsString();

	public void setAttributesAsString(String str);
}
