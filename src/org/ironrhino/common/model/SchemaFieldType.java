package org.ironrhino.common.model;

import org.ironrhino.core.model.Displayable;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.util.LocalizedTextUtil;

public enum SchemaFieldType implements Displayable {
	SELECT, CHECKBOX, INPUT, GROUP;
	@Override
	public String getName() {
		return name();
	}

	@Override
	public String getDisplayName() {
		try {
			return LocalizedTextUtil.findText(getClass(), name(), ActionContext
					.getContext().getLocale(), name(), null);
		} catch (Exception e) {
			return name();
		}
	}

	public static SchemaFieldType parse(String name) {
		if (name != null)
			for (SchemaFieldType en : values())
				if (name.equals(en.name()) || name.equals(en.getDisplayName()))
					return en;
		return null;
	}

	@Override
	public String toString() {
		return getDisplayName();
	}
}
