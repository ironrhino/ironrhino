package org.ironrhino.core.util;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.persistence.Lob;

import org.ironrhino.core.metadata.NotInJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;

public class JsonUtils {

	public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
	public static String[] ACCEPT_DATE_FORMATS = { DEFAULT_DATE_FORMAT,
			"yyyy-MM-dd'T'HH:mm:ss", "yyyy/MM/dd", "yyyy-MM-dd" };

	public static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<List<String>>() {
	};

	public static final TypeReference<Map<String, String>> STRING_MAP_TYPE = new TypeReference<Map<String, String>>() {
	};

	private static Logger log = LoggerFactory.getLogger(JsonUtils.class);

	private static ObjectMapper objectMapper = createNewObjectMapper();

	public static ObjectMapper getObjectMapper() {
		return objectMapper;
	}

	public static ObjectMapper createNewObjectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setDateFormat(new SimpleDateFormat(DEFAULT_DATE_FORMAT));
		objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		objectMapper
				.setAnnotationIntrospector(new JacksonAnnotationIntrospector() {

					private static final long serialVersionUID = 8855888602140931060L;

					@Override
					protected boolean _isIgnorable(Annotated a) {
						boolean b = super._isIgnorable(a);
						if (!b) {
							NotInJson notInJson = a
									.getAnnotation(NotInJson.class);
							b = notInJson != null;
							if (!b) {
								Lob lob = a.getAnnotation(Lob.class);
								b = lob != null;
							}
						}
						return b;
					}

				});
		objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
		return objectMapper;
	}

	public static String toJson(Object object) {
		try {
			return objectMapper.writeValueAsString(object);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}

	public static boolean isValidJson(String content) {
		try {
			getObjectMapper().readValue(content, JsonNode.class);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T fromJson(String json, TypeReference<T> type)
			throws JsonParseException, JsonMappingException, IOException {
		return (T) objectMapper.readValue(json, type);
	}

	@SuppressWarnings("unchecked")
	public static <T> T fromJson(String json, Class<T> cls)
			throws JsonParseException, JsonMappingException, IOException {
		if (Date.class.isAssignableFrom(cls)) {
			if (org.apache.commons.lang3.StringUtils.isNumeric(json)) {
				Calendar cal = Calendar.getInstance();
				cal.setTimeInMillis(Long.valueOf(json));
				return (T) cal.getTime();
			} else if (json.startsWith("\"") && json.endsWith("\"")) {
				String value = json.substring(1, json.length() - 2);
				for (String pattern : ACCEPT_DATE_FORMATS) {
					DateFormat format = new SimpleDateFormat(pattern);
					try {
						return (T) format.parse(value);
					} catch (Exception e) {
						continue;
					}
				}
			}
			return null;
		} else {
			return objectMapper.readValue(json, cls);
		}
	}

	public static <T> T fromJson(String json, Type type)
			throws JsonParseException, JsonMappingException, IOException {
		return objectMapper.readValue(json, objectMapper
				.getDeserializationConfig().getTypeFactory()
				.constructType(type));
	}

	public static String unprettify(String json) {
		ObjectMapper objectMapper = getObjectMapper();
		try {
			JsonNode node = objectMapper.readValue(json, JsonNode.class);
			return objectMapper.writeValueAsString(node);
		} catch (Exception e) {
			return json;
		}
	}

	public static String prettify(String json) {
		ObjectMapper objectMapper = getObjectMapper();
		try {
			JsonNode node = objectMapper.readValue(json, JsonNode.class);
			ObjectWriter writer = objectMapper
					.writer(new DefaultPrettyPrinter());
			return writer.writeValueAsString(node);
		} catch (Exception e) {
			return json;
		}
	}

}
