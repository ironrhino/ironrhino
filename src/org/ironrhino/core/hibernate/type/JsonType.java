
package org.ironrhino.core.hibernate.type;

import java.util.Properties;

import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.usertype.DynamicParameterizedType;

public class JsonType extends AbstractSingleColumnStandardBasicType<Object> implements DynamicParameterizedType {

	private static final long serialVersionUID = -6106597335909896629L;

	public JsonType() {
		super(JsonSqlTypeDescriptor.INSTANCE, new JsonJavaTypeDescriptor());
	}

	@Override
	public String getName() {
		return "json";
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

	@Override
	public void setParameterValues(Properties parameters) {
		((JsonJavaTypeDescriptor) getJavaTypeDescriptor()).setParameterValues(parameters);
	}
}