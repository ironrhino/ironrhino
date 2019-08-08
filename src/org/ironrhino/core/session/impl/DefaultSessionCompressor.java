package org.ironrhino.core.session.impl;

import org.ironrhino.core.session.SessionCompressor;
import org.ironrhino.core.util.JsonSerializationUtils;
import org.springframework.security.web.savedrequest.SavedRequest;

public class DefaultSessionCompressor implements SessionCompressor<Object> {

	@Override
	public boolean supportsKey(String key) {
		return false;
	}

	@Override
	public String compress(Object object) throws Exception {
		if (object instanceof SavedRequest)
			return null;
		return JsonSerializationUtils.serialize(object);
	}

	@Override
	public Object uncompress(String string) throws Exception {
		return JsonSerializationUtils.deserialize(string);
	}

}
