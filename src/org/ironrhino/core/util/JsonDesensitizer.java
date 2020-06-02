package org.ironrhino.core.util;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiPredicate;
import java.util.function.Function;

import org.ironrhino.core.metadata.JsonDesensitize;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.core.annotation.AnnotationUtils;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import lombok.Getter;

public class JsonDesensitizer {

	public static JsonDesensitizer DEFAULT_INSTANCE = new JsonDesensitizer();

	@Getter
	private final Map<BiPredicate<String, Object>, Function<String, String>> mapping;

	@Getter
	private final List<BiPredicate<String, Object>> dropping;

	private final ObjectWriter objectWriter;

	@JsonFilter("desensitizer")
	static class DesensitizerMixIn {

	}

	public JsonDesensitizer() {
		this(new ConcurrentHashMap<>());
		getMapping().put((s, obj) -> s.equals("password") || s.endsWith("Password") || s.endsWith("Passwords"),
				s -> "******");
	}

	public JsonDesensitizer(Map<BiPredicate<String, Object>, Function<String, String>> mapping) {
		this(mapping, new CopyOnWriteArrayList<>());
	}

	public JsonDesensitizer(Map<BiPredicate<String, Object>, Function<String, String>> mapping,
			List<BiPredicate<String, Object>> dropping) {
		this.mapping = mapping;
		this.dropping = dropping;
		FilterProvider filters = new SimpleFilterProvider().setDefaultFilter(new SimpleBeanPropertyFilter() {
			@Override
			public void serializeAsField(Object obj, JsonGenerator jgen, SerializerProvider provider,
					PropertyWriter writer) throws Exception {
				String name = writer.getName();
				if (include(writer) && !dropping.stream().anyMatch(entry -> entry.test(name, obj))) {
					BeanWrapperImpl bw = new BeanWrapperImpl(obj);
					Optional<Function<String, String>> func = mapping.entrySet().stream()
							.filter(entry -> entry.getKey().test(name, obj)).findFirst().map(entry -> entry.getValue());
					if (func.isPresent()) {
						Object value = bw.getPropertyValue(name);
						try {
							String newValue = func.get().apply(value != null ? String.valueOf(value) : null);
							Class<?> type = bw.getPropertyType(name);
							if (TypeUtils.isNumeric(type) && NumberUtils.isNumber(newValue)) {
								jgen.writeFieldName(name);
								jgen.writeNumber(newValue);
							} else if ((type == Boolean.class || type == boolean.class)
									&& ("true".equals(newValue) || "false".equals(newValue))) {
								jgen.writeBooleanField(name, Boolean.getBoolean(newValue));
							} else {
								jgen.writeStringField(name, newValue);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else {
						JsonDesensitize annotation = null;
						try {
							annotation = AnnotationUtils.findAnnotation(bw.getPropertyDescriptor(name).getReadMethod(),
									JsonDesensitize.class);
							if (annotation == null)
								annotation = AnnotationUtils.findAnnotation(
										ReflectionUtils.getField(obj.getClass(), name), JsonDesensitize.class);
						} catch (Exception e) {

						}
						if (annotation != null) {
							String newValue = annotation.value();
							if (newValue.equals(JsonDesensitize.DEFAULT_NONE)) {
								writer.serializeAsOmittedField(obj, jgen, provider);
							} else {
								Class<?> type = bw.getPropertyType(name);
								if (TypeUtils.isNumeric(type) && NumberUtils.isNumber(newValue)) {
									jgen.writeFieldName(name);
									jgen.writeNumber(newValue);
								} else if ((type == Boolean.class || type == boolean.class)
										&& ("true".equals(newValue) || "false".equals(newValue))) {
									jgen.writeBooleanField(name, Boolean.getBoolean(newValue));
								} else {
									jgen.writeStringField(name, newValue);
								}
							}
						} else {
							writer.serializeAsField(obj, jgen, provider);
						}
					}
				} else if (!jgen.canOmitFields()) {
					writer.serializeAsOmittedField(obj, jgen, provider);
				}
			}
		}).setFailOnUnknownId(false);
		objectWriter = JsonUtils.createNewObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
				.addMixIn(Object.class, DesensitizerMixIn.class).writer(filters);
	}

	public String toJson(Object value) {
		try {
			return objectWriter.writeValueAsString(value);
		} catch (Exception e) {
			return null;
		}
	}

	public String desensitize(String json) {
		try {
			JsonNode node = JsonUtils.getObjectMapper().readTree(json);
			desensitize(null, node, null);
			return JsonUtils.getObjectMapper().writeValueAsString(node);
		} catch (IOException e) {
			return json;
		}
	}

	private void desensitize(String nodeName, JsonNode node, JsonNode parent) {
		if (node.isObject()) {
			List<String> toBeDropped = new ArrayList<>();
			node.fieldNames().forEachRemaining(name -> {
				if (dropping.stream().anyMatch(p -> p.test(name, node)))
					toBeDropped.add(name);
			});
			((ObjectNode) node).remove(toBeDropped);
			Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();
			while (iterator.hasNext()) {
				Map.Entry<String, JsonNode> entry = iterator.next();
				desensitize(entry.getKey(), entry.getValue(), node);
			}
		} else if (node.isArray()) {
			Iterator<JsonNode> iterator = node.elements();
			while (iterator.hasNext()) {
				JsonNode element = iterator.next();
				desensitize(null, element, node);
			}
		} else if (parent instanceof ObjectNode) {
			mapping.forEach((k, v) -> {
				if (k.test(nodeName, parent)) {
					ObjectNode on = ((ObjectNode) parent);
					String value = v.apply(node.isNull() ? null : node.asText());
					try {
						if (node.isNumber())
							on.put(nodeName, new BigDecimal(value));
						else if (node.isBoolean())
							on.put(nodeName, Boolean.valueOf(value));
						else
							on.put(nodeName, value);
					} catch (Exception e) {
						on.put(nodeName, value);
					}
				}
			});
		}
	}

}
