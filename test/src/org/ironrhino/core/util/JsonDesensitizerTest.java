package org.ironrhino.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;

import org.junit.Test;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class JsonDesensitizerTest {

	@Test
	public void testDesensitize() {
		String json = "{\"password\":\"password\"}";
		assertEquals("{\"password\":\"******\"}", JsonDesensitizer.DEFAULT_INSTANCE.desensitize(json));
		json = "{\"username\":\"username\",\"password\":\"password\"}";
		assertEquals("{\"username\":\"username\",\"password\":\"******\"}",
				JsonDesensitizer.DEFAULT_INSTANCE.desensitize(json));
		json = "{\"user\":{\"username\":\"username\",\"password\":\"password\"}}";
		assertEquals("{\"user\":{\"username\":\"username\",\"password\":\"******\"}}",
				JsonDesensitizer.DEFAULT_INSTANCE.desensitize(json));
	}

	@Test
	public void testToJson() {
		assertTrue(JsonDesensitizer.DEFAULT_INSTANCE.toJson(new User("username", "password", 12)).contains("******"));
	}

	@Test
	public void testCustomize() {
		JsonDesensitizer desensitizer = new JsonDesensitizer();
		Map<BiPredicate<String, Object>, Function<String, String>> mapping = desensitizer.getMapping();
		mapping.clear();
		assertFalse(desensitizer.toJson(new User("username", "password", 12)).contains("******"));
		mapping.put((s, obj) -> s.equals("username") && obj instanceof User, s -> "------");
		mapping.put((s, obj) -> s.equals("age") && obj instanceof User, s -> "0.0");
		String json = desensitizer.toJson(new User("myname", "mypass", 12));
		assertTrue(json.contains("------"));
		assertTrue(json.contains("\"mypass\""));
		assertFalse(json.contains("12"));
		assertTrue(json.contains("0.0"));

	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	static class User {
		String username;
		String password;
		int age;
	}

}
