package org.ironrhino.rest.client;

import com.fasterxml.jackson.databind.JsonNode;

@FunctionalInterface
public interface JsonValidator {

	void validate(JsonNode tree);

}
