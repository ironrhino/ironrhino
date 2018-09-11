package org.ironrhino.core.session.impl;

import org.ironrhino.core.session.SessionCompressor;
import org.ironrhino.core.util.JsonSerializationUtils;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DefaultSessionCompressor implements SessionCompressor<Object> {

	private final ObjectMapper objectMapper;

	public DefaultSessionCompressor() {
		objectMapper = JsonSerializationUtils.createNewObjectMapper()
				.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
	}

	@Override
	public boolean supportsKey(String key) {
		return false;
	}

	@Override
	public String compress(Object value) throws Exception {
		if (value == null)
			return null;
		if (value instanceof Long)
			return value + "L";
		else if (value instanceof Float)
			return value + "F";
		return objectMapper.writeValueAsString(value);
	}

	@Override
	public Object uncompress(String string) throws Exception {
		if (string == null || string.isEmpty())
			return string;
		if (Character.isDigit(string.charAt(0))) {
			if (string.endsWith("L"))
				return Long.valueOf(string.substring(0, string.length() - 1));
			else if (string.endsWith("F"))
				return Float.valueOf(string.substring(0, string.length() - 1));
		}
		return objectMapper.readValue(string, Object.class);
	}

}
