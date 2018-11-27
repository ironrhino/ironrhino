package org.ironrhino.core.util;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.persistence.Lob;

import org.ironrhino.core.model.Displayable;
import org.ironrhino.core.util.AppInfo.Stage;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.jackson2.SimpleGrantedAuthorityMixin;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
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
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.module.SimpleModule;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class JsonUtils {

	public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
	public static final String DEFAULT_TIME_FORMAT = "HH:mm:ss";

	private static final DateTimeFormatter DEFAULT_DATETIME_FORMATTER = DateTimeFormatter
			.ofPattern(DEFAULT_DATE_FORMAT);
	private static final DateTimeFormatter DEFAULT_TIME_FORMATTER = DateTimeFormatter.ofPattern(DEFAULT_TIME_FORMAT);

	public static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<List<String>>() {
	};

	public static final TypeReference<Map<String, String>> STRING_MAP_TYPE = new TypeReference<Map<String, String>>() {
	};

	public static final Module MODULE_TEMPORAL = new SimpleModule()
			.addDeserializer(Date.class, new JsonDeserializer<Date>() {
				@Override
				public Date deserialize(JsonParser jsonparser, DeserializationContext deserializationcontext)
						throws IOException, JsonProcessingException {
					String date = jsonparser.getText();
					DateFormat df = deserializationcontext.getConfig().getDateFormat();
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
			}).addDeserializer(LocalDate.class, new JsonDeserializer<LocalDate>() {
				@Override
				public LocalDate deserialize(JsonParser jsonparser, DeserializationContext deserializationcontext)
						throws IOException, JsonProcessingException {
					String date = jsonparser.getText();
					DateFormat df = deserializationcontext.getConfig().getDateFormat();
					if (df != null) {
						DateFormat clone = (DateFormat) df.clone();
						try {
							return clone.parse(date).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
						} catch (ParseException e) {
						}
					}
					Date d = DateUtils.parse(date);
					if (d == null)
						throw new RuntimeException(date + " is not valid date");
					return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
				}
			}).addSerializer(LocalDate.class, new JsonSerializer<LocalDate>() {
				@Override
				public void serialize(LocalDate localDate, JsonGenerator jsonGenerator,
						SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
					jsonGenerator.writeString(localDate.toString());
				}
			}).addDeserializer(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
				@Override
				public LocalDateTime deserialize(JsonParser jsonparser, DeserializationContext deserializationcontext)
						throws IOException, JsonProcessingException {
					String date = jsonparser.getText();
					DateFormat df = deserializationcontext.getConfig().getDateFormat();
					if (df != null) {
						DateFormat clone = (DateFormat) df.clone();
						try {
							return clone.parse(date).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
						} catch (ParseException e) {
						}
					}
					Date d = DateUtils.parse(date);
					if (d == null)
						throw new RuntimeException(date + " is not valid date");
					return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
				}
			}).addSerializer(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
				@Override
				public void serialize(LocalDateTime localDateTime, JsonGenerator jsonGenerator,
						SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
					jsonGenerator.writeString(DEFAULT_DATETIME_FORMATTER.format(localDateTime));
				}
			}).addDeserializer(LocalTime.class, new JsonDeserializer<LocalTime>() {
				@Override
				public LocalTime deserialize(JsonParser jsonparser, DeserializationContext deserializationcontext)
						throws IOException, JsonProcessingException {
					return LocalTime.parse(jsonparser.getText());
				}
			}).addSerializer(LocalTime.class, new JsonSerializer<LocalTime>() {
				@Override
				public void serialize(LocalTime localTime, JsonGenerator jsonGenerator,
						SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
					jsonGenerator.writeString(DEFAULT_TIME_FORMATTER.format(localTime));
				}
			}).addDeserializer(YearMonth.class, new JsonDeserializer<YearMonth>() {
				@Override
				public YearMonth deserialize(JsonParser jsonparser, DeserializationContext deserializationcontext)
						throws IOException, JsonProcessingException {
					return YearMonth.parse(jsonparser.getText());
				}
			}).addSerializer(YearMonth.class, new JsonSerializer<YearMonth>() {
				@Override
				public void serialize(YearMonth yearMonth, JsonGenerator jsonGenerator,
						SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
					jsonGenerator.writeString(yearMonth.toString());
				}
			}).addDeserializer(Duration.class, new JsonDeserializer<Duration>() {
				@Override
				public Duration deserialize(JsonParser jsonparser, DeserializationContext deserializationcontext)
						throws IOException, JsonProcessingException {
					return Duration.parse(jsonparser.getText());
				}
			}).addSerializer(Duration.class, new JsonSerializer<Duration>() {
				@Override
				public void serialize(Duration duration, JsonGenerator jsonGenerator,
						SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
					jsonGenerator.writeString(duration.toString());
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
		objectMapper.registerModule(MODULE_TEMPORAL);
		if (AppInfo.getStage() == Stage.DEVELOPMENT)
			objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
		return objectMapper;
	}

	public static String toJson(Object object) {
		try {
			return sharedObjectMapper.writeValueAsString(object);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}

	public static String toJsonWithView(Object object, Class<?> serializationView) {
		try {
			return sharedObjectMapper.writerWithView(serializationView).writeValueAsString(object);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
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

	@SuppressWarnings("unchecked")
	public static <T> T fromJson(String json, TypeReference<T> type)
			throws JsonParseException, JsonMappingException, IOException {
		return (T) sharedObjectMapper.readValue(json, type);
	}

	@SuppressWarnings("unchecked")
	public static <T> T fromJson(JsonNode json, TypeReference<T> type)
			throws JsonParseException, JsonMappingException, IOException {
		return (T) sharedObjectMapper.readValue(sharedObjectMapper.treeAsTokens(json), type);
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
