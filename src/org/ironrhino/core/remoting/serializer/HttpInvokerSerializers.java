package org.ironrhino.core.remoting.serializer;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.util.ClassUtils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public final class HttpInvokerSerializers {

	public static final HttpInvokerSerializer DEFAULT_SERIALIZER = JavaHttpInvokerSerializer.INSTANCE;

	public static final List<HttpInvokerSerializer> SERIALIZERS = new ArrayList<>();

	static {
		SERIALIZERS.add(DEFAULT_SERIALIZER);
		SERIALIZERS.add(JsonHttpInvokerSerializer.INSTANCE);
		if (ClassUtils.isPresent("com.fasterxml.jackson.dataformat.smile.SmileFactory",
				HttpInvokerSerializers.class.getClassLoader()))
			SERIALIZERS.add(SmileHttpInvokerSerializer.INSTANCE);
		if (ClassUtils.isPresent("org.nustaq.serialization.FSTObjectInput",
				HttpInvokerSerializers.class.getClassLoader()))
			SERIALIZERS.add(FstHttpInvokerSerializer.INSTANCE);
	}

	public static HttpInvokerSerializer forRequest(HttpServletRequest request) {
		return ofContentType(request.getHeader(HttpHeaders.CONTENT_TYPE));
	}

	public static HttpInvokerSerializer ofContentType(String contentType) {
		if (StringUtils.isNotBlank(contentType)) {
			for (HttpInvokerSerializer serializer : SERIALIZERS)
				if (contentType.startsWith(serializer.getContentType()))
					return serializer;
		}
		log.warn("Use default {} for unknown contentType: {}", DEFAULT_SERIALIZER.getClass().getName(), contentType);
		return DEFAULT_SERIALIZER;
	}

	public static HttpInvokerSerializer ofSerializationType(String serializationType) {
		if (StringUtils.isNotBlank(serializationType)) {
			for (HttpInvokerSerializer serializer : SERIALIZERS)
				if (serializer.getSerializationType().equalsIgnoreCase(serializationType))
					return serializer;
			log.warn("Use default {} for unknown serializationType: {}", DEFAULT_SERIALIZER.getClass().getName(),
					serializationType);
		}
		return DEFAULT_SERIALIZER;
	}

}
