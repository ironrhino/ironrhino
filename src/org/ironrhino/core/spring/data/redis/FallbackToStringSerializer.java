package org.ironrhino.core.spring.data.redis;

import java.io.EOFException;
import java.io.ObjectStreamConstants;
import java.io.StreamCorruptedException;
import java.nio.charset.StandardCharsets;

import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

public class FallbackToStringSerializer extends JdkSerializationRedisSerializer {

	@Override
	public byte[] serialize(Object object) {
		if (object instanceof String)
			return ((String) object).getBytes(StandardCharsets.UTF_8);
		return super.serialize(object);
	}

	@Override
	public Object deserialize(byte[] bytes) {
		try {
			return super.deserialize(bytes);
		} catch (SerializationException se) {
			if (!isJavaSerialized(bytes) && se.getCause() instanceof SerializationFailedException) {
				Throwable cause = se.getCause().getCause();
				if ((cause instanceof StreamCorruptedException || cause instanceof EOFException)
						&& org.ironrhino.core.util.StringUtils.isUtf8(bytes))
					return new String(bytes, StandardCharsets.UTF_8);
			}
			throw se;
		}
	}

	private static boolean isJavaSerialized(byte[] bytes) {
		if (bytes.length > 2) {
			short magic = (short) ((bytes[1] & 0xFF) + (bytes[0] << 8));
			return magic == ObjectStreamConstants.STREAM_MAGIC;
		}
		return false;
	}
}