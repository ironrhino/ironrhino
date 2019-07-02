package org.ironrhino.rest.client;

import com.fasterxml.jackson.databind.JsonNode;

public interface JsonValidator {

	void validate(JsonNode tree);

}
