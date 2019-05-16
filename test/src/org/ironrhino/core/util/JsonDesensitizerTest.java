package org.ironrhino.core.util;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;

import org.hamcrest.Matcher;
import org.junit.Test;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class JsonDesensitizerTest {

	private static final Matcher<String> containsMask = containsString("\"******\"");

	@Test
	public void testDesensitize() {
		String json = "{\"password\":\"password\"}";
		assertThat(JsonDesensitizer.DEFAULT_INSTANCE.desensitize(json), containsMask);
		json = "{\"username\":\"username\",\"password\":\"password\"}";
		assertThat(JsonDesensitizer.DEFAULT_INSTANCE.desensitize(json), containsMask);
		json = "{\"user\":{\"username\":\"username\",\"password\":\"password\"}}";
		assertThat(JsonDesensitizer.DEFAULT_INSTANCE.desensitize(json), containsMask);
	}

	@Test
	public void testToJson() {
		assertThat(JsonDesensitizer.DEFAULT_INSTANCE.toJson(new User("username", "password", 12)), containsMask);
	}

	@Test
	public void testCustomize() {
		JsonDesensitizer desensitizer = new JsonDesensitizer();
		Map<BiPredicate<String, Object>, Function<String, String>> mapping = desensitizer.getMapping();
		mapping.clear();
		assertThat(desensitizer.toJson(new User("username", "password", 12)), not(containsMask));
		mapping.put((s, obj) -> s.equals("username") && obj instanceof User, s -> "------");
		mapping.put((s, obj) -> s.equals("age") && obj instanceof User, s -> "0.0");
		String json = desensitizer.toJson(new User("myname", "mypass", 12));
		assertThat(json, containsString("------"));
		assertThat(json, containsString("\"mypass\""));
		assertThat(json, not(containsString("12")));
		assertThat(json, containsString("0.0"));
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
