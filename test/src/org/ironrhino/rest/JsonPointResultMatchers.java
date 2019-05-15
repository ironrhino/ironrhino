package org.ironrhino.rest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.UnsupportedEncodingException;

import org.ironrhino.core.util.JsonUtils;
import org.springframework.test.util.AssertionErrors;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;

public class JsonPointResultMatchers {

	private String expression;

	protected JsonPointResultMatchers(String expression) {
		this.expression = expression;
	}

	public ResultMatcher value(Object expectedValue) {
		return result -> {
			JsonNode jsonNode = JsonUtils.getObjectMapper().readTree(getContent(result)).at(expression);
			if (jsonNode == MissingNode.getInstance()) {
				if (expectedValue != null) {
					assertThat("Json point \"" + this.expression + "\"", null, is(expectedValue));
				}
				return;
			}
			Object actualValue = JsonUtils.fromJson(jsonNode, expectedValue.getClass());
			AssertionErrors.assertEquals("JSON point \"" + this.expression + "\"", expectedValue, actualValue);
		};
	}

	private String getContent(MvcResult result) throws UnsupportedEncodingException {
		return result.getResponse().getContentAsString();
	}
}
