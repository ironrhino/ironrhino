package org.ironrhino.core.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class JsonDesensitizer {

	public static JsonDesensitizer DEFAULT_INSTANCE = new JsonDesensitizer();

	private final Map<Predicate<String>, Function<String, String>> mapper;

	public JsonDesensitizer() {
		mapper = new HashMap<>();
		mapper.put(s -> s.equals("password") || s.endsWith("Password"), s -> "******");
	}

	public JsonDesensitizer(Map<Predicate<String>, Function<String, String>> mapper) {
		this.mapper = mapper;
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
			mapper.entrySet().stream().forEach(entry -> {
				if (entry.getKey().test(nodeName))
					((ObjectNode) parent).put(nodeName, entry.getValue().apply(node.asText()));
			});
		}
	}

}
