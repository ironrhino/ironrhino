package org.ironrhino.core.util;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.persistence.Lob;

import org.ironrhino.common.model.Coordinate;
import org.ironrhino.core.model.Displayable;
import org.ironrhino.core.util.AppInfo.Stage;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.jackson2.SimpleGrantedAuthorityMixin;
import org.springframework.util.ClassUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.module.SimpleModule;

import lombok.experimental.UtilityClass;

@UtilityClass
public class JsonUtils {

	public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
	public static final String DEFAULT_TIME_FORMAT = "HH:mm:ss";

	public static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<List<String>>() {
	};

	public static final TypeReference<Map<String, String>> STRING_MAP_TYPE = new TypeReference<Map<String, String>>() {
	};

	public static final Module MODULE_COMMON = new SimpleModule()
			.addDeserializer(Date.class, new JsonDeserializer<Date>() {
				@Override
				public Date deserialize(JsonParser parser, DeserializationContext ctx)
						throws IOException, JsonProcessingException {
					String date = parser.getText();
					DateFormat df = ctx.getConfig().getDateFormat();
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
			}).addDeserializer(Coordinate.class, new JsonDeserializer<Coordinate>() {
				@Override
				public Coordinate deserialize(JsonParser parser, DeserializationContext ctx)
						throws IOException, JsonProcessingException {
					if (parser.currentToken() == JsonToken.START_ARRAY) {
						Double[] array = parser.readValueAs(new TypeReference<Double[]>() {
						});
						return new Coordinate(array[0], array[1]);
					} else if (parser.currentToken() == JsonToken.VALUE_STRING) {
						return new Coordinate(parser.getText());
					} else {
						Map<String, Double> map = parser.readValueAs(new TypeReference<Map<String, Double>>() {
						});
						return new Coordinate(map.get("latitude"), map.get("longitude"));
					}
				}
			});

	private static final ObjectMapper sharedObjectMapper = createNewObjectMapper();

	public static ObjectMapper getObjectMapper() {
		return sharedObjectMapper;
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
		objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		objectMapper.setTimeZone(TimeZone.getDefault());
		objectMapper.addMixIn(GrantedAuthority.class, SimpleGrantedAuthorityMixin.class)
				.addMixIn(SimpleGrantedAuthority.class, SimpleGrantedAuthorityMixin.class);
		objectMapper.registerModule(MODULE_COMMON);
		if (ClassUtils.isPresent("com.fasterxml.jackson.datatype.jsr310.JavaTimeModule",
				JsonUtils.class.getClassLoader())) {
			objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
		}
		if (ClassUtils.isPresent("com.fasterxml.jackson.module.paramnames.ParameterNamesModule",
				JsonUtils.class.getClassLoader())) {
			objectMapper.registerModule(new com.fasterxml.jackson.module.paramnames.ParameterNamesModule());
		}
		if (ClassUtils.isPresent("com.fasterxml.jackson.module.mrbean.MrBeanModule",
				JsonUtils.class.getClassLoader())) {
			objectMapper.registerModule(new com.fasterxml.jackson.module.mrbean.MrBeanModule());
		}
		if (AppInfo.getStage() == Stage.DEVELOPMENT)
			objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
		return objectMapper;
	}

	public static String toJson(Object object) {
		try {
			return sharedObjectMapper.writeValueAsString(object);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	public static String toJsonWithView(Object object, Class<?> serializationView) {
		try {
			return sharedObjectMapper.writerWithView(serializationView).writeValueAsString(object);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static boolean isValidJson(String content) {
		try {
			sharedObjectMapper.readTree(content);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static <T> T fromJson(String json, TypeReference<T> type)
			throws JsonParseException, JsonMappingException, IOException {
		return sharedObjectMapper.readValue(json, type);
	}

	public static <T> T fromJson(JsonNode json, TypeReference<T> type)
			throws JsonParseException, JsonMappingException, IOException {
		return sharedObjectMapper.readValue(sharedObjectMapper.treeAsTokens(json), type);
	}

	public static <T> T fromJson(String json, Class<T> cls)
			throws JsonParseException, JsonMappingException, IOException {
		return sharedObjectMapper.readValue(json, cls);
	}

	public static <T> T fromJson(JsonNode json, Class<T> cls)
			throws JsonParseException, JsonMappingException, IOException {
		return sharedObjectMapper.treeToValue(json, cls);
	}

	public static <T> T fromJson(String json, Type type) throws JsonParseException, JsonMappingException, IOException {
		return sharedObjectMapper.readValue(json, sharedObjectMapper.constructType(type));
	}

	@SuppressWarnings("unchecked")
	public static <T> T fromJson(JsonNode json, Type type)
			throws JsonParseException, JsonMappingException, IOException {
		if (type instanceof Class && ((Class<?>) type).isAssignableFrom(JsonNode.class))
			return (T) json;
		return sharedObjectMapper.readValue(sharedObjectMapper.treeAsTokens(json),
				sharedObjectMapper.constructType(type));
	}

	public static <T extends Enum<T>> String enumToJson(Class<T> clazz) {
		if (Displayable.class.isAssignableFrom(clazz)) {
			return JsonUtils.toJson(EnumUtils.enumToMap(clazz));
		} else {
			return JsonUtils.toJson(EnumUtils.enumToList(clazz));
		}
	}

	public static String unprettify(String json) {
		try {
			JsonNode node = sharedObjectMapper.readTree(json);
			return sharedObjectMapper.writeValueAsString(node);
		} catch (Exception e) {
			return json;
		}
	}

	public static String prettify(String json) {
		try {
			JsonNode node = sharedObjectMapper.readTree(json);
			ObjectWriter writer = sharedObjectMapper.writer(new DefaultPrettyPrinter());
			return writer.writeValueAsString(node);
		} catch (Exception e) {
			return json;
		}
	}

}
