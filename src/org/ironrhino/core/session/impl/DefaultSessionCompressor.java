package org.ironrhino.core.session.impl;

import org.ironrhino.core.session.SessionCompressor;
import org.ironrhino.core.util.JsonSerializationUtils;

public class DefaultSessionCompressor implements SessionCompressor<Object> {

	@Override
	public boolean supportsKey(String key) {
		return false;
	}

	@Override
	public String compress(Object object) throws Exception {
		return JsonSerializationUtils.serialize(object);
	}

	@Override
	public Object uncompress(String string) throws Exception {
		return JsonSerializationUtils.deserialize(string);
	}

}
