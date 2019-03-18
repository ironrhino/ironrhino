package org.ironrhino.core.util;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.Function;

import org.springframework.beans.BeanWrapperImpl;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import lombok.Getter;

public class JsonDesensitizer {

	public static JsonDesensitizer DEFAULT_INSTANCE = new JsonDesensitizer();

	@Getter
	private final Map<BiPredicate<String, Object>, Function<String, String>> mapping;

	private final ObjectWriter objectWriter;

	@JsonFilter("desensitizer")
	static class DesensitizerMixIn {

	}

	public JsonDesensitizer() {
		this(new ConcurrentHashMap<BiPredicate<String, Object>, Function<String, String>>() {
			private static final long serialVersionUID = 1L;
			{
				put((s, obj) -> s.equals("password") || s.endsWith("Password"), s -> "******");
			}
		});
	}

	public JsonDesensitizer(Map<BiPredicate<String, Object>, Function<String, String>> mapper) {
		this.mapping = mapper;
		FilterProvider filters = new SimpleFilterProvider().setDefaultFilter(new SimpleBeanPropertyFilter() {
			@Override
			public void serializeAsField(Object obj, JsonGenerator jgen, SerializerProvider provider,
					PropertyWriter writer) throws Exception {
				if (include(writer)) {
					String name = writer.getName();
					Optional<Function<String, String>> func = mapper.entrySet().stream()
							.filter(entry -> entry.getKey().test(name, obj)).findFirst().map(entry -> entry.getValue());
					if (func.isPresent()) {
						Object value = new BeanWrapperImpl(obj).getPropertyValue(name);
						if (value instanceof String) {
							try {
								jgen.writeStringField(name, func.get().apply((String) value));
							} catch (IOException e) {
								e.printStackTrace();
							}
						} else {
							writer.serializeAsField(obj, jgen, provider);
						}
					} else {
						writer.serializeAsField(obj, jgen, provider);
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
		} else if (parent instanceof ObjectNode && node instanceof TextNode) {
			mapping.entrySet().stream().forEach(entry -> {
				if (entry.getKey().test(nodeName, parent))
					((ObjectNode) parent).put(nodeName, entry.getValue().apply(node.asText()));
			});
		}
	}

}
