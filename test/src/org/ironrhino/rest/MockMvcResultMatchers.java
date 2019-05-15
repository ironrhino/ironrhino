package org.ironrhino.rest;

public abstract class MockMvcResultMatchers {

	public static JsonPointResultMatchers jsonPoint(String expression) {
		return new JsonPointResultMatchers(expression);
	}

}
