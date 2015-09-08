package org.ironrhino.core.util;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.persistence.Lob;

import org.ironrhino.core.model.Displayable;
import org.ironrhino.core.util.AppInfo.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class JsonUtils {

	public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

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
		final ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setDateFormat(new SimpleDateFormat(DEFAULT_DATE_FORMAT));
		objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		objectMapper.setAnnotationIntrospector(new JacksonAnnotationIntrospector() {

			private static final long serialVersionUID = 8855888602140931060L;

			@Override
			protected boolean _isIgnorable(Annotated a) {
				boolean b = super._isIgnorable(a);
				if (!b) {
					Lob lob = a.getAnnotation(Lob.class);
					b = lob != null && a.getAnnotation(JsonProperty.class) == null;
				}
				return b;
			}

		});
		objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
		objectMapper.registerModule(new SimpleModule().addDeserializer(Date.class, new JsonDeserializer<Date>() {
			@Override
			public Date deserialize(JsonParser jsonparser, DeserializationContext deserializationcontext)
					throws IOException, JsonProcessingException {
				String date = jsonparser.getText();
				DateFormat df = objectMapper.getDeserializationConfig().getDateFormat();
				if (df != null) {
					DateFormat clone = (DateFormat) df.clone();
					try {
						return clone.parse(date);
					} catch (ParseException e) {
					}
				}
				Date d = DateUtils.parse(date);
				if (d == null)
					throw new RuntimeException(date + " is not valid date");
				return d;
			}
		}));
		if (AppInfo.getStage() == Stage.DEVELOPMENT)
			objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
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

	public static <T> T fromJson(String json, Class<T> cls)
			throws JsonParseException, JsonMappingException, IOException {
		return objectMapper.readValue(json, cls);
	}

	public static <T> T fromJson(String json, Type type) throws JsonParseException, JsonMappingException, IOException {
		return objectMapper.readValue(json,
				objectMapper.getDeserializationConfig().getTypeFactory().constructType(type));
	}

	public static <T extends Enum<T>> String enumToJson(Class<T> clazz) {
		if (Displayable.class.isAssignableFrom(clazz)) {
			return JsonUtils.toJson(EnumUtils.enumToMap(clazz));
		} else {
			return JsonUtils.toJson(EnumUtils.enumToList(clazz));
		}
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
			ObjectWriter writer = objectMapper.writer(new DefaultPrettyPrinter());
			return writer.writeValueAsString(node);
		} catch (Exception e) {
			return json;
		}
	}

}
