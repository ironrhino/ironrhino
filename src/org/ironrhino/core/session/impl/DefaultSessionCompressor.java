package org.ironrhino.core.session.impl;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.session.SessionCompressor;
import org.ironrhino.core.util.JsonUtils;
import org.ironrhino.core.util.ReflectionUtils;

public class DefaultSessionCompressor implements SessionCompressor<Object> {

	private static final String SEPERATOR = "@";

	@Override
	public boolean supportsKey(String key) {
		return false;
	}

	@Override
	public String compress(Object value) throws Exception {
		if (value == null)
			return null;
		if (value instanceof String)
			return (String) value;
		return ReflectionUtils.getActualClass(value).getName() + SEPERATOR + JsonUtils.toJson(value);
	}

	@Override
	public Object uncompress(String string) throws Exception {
		if (StringUtils.isBlank(string))
			return null;
		int index = string.indexOf(SEPERATOR);
		if (index < 0)
			return string;
		try {
			String className = string.substring(0, index);
			String json = string.substring(index + SEPERATOR.length());
			return JsonUtils.fromJson(json, Class.forName(className));
		} catch (ClassNotFoundException e) {
			return string;
		}

	}

}
