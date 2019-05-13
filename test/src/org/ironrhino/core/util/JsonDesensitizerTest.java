package org.ironrhino.core.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

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
		assertThat(JsonDesensitizer.DEFAULT_INSTANCE.desensitize(json), equalTo("{\"password\":\"******\"}"));
		json = "{\"username\":\"username\",\"password\":\"password\"}";
		assertThat(JsonDesensitizer.DEFAULT_INSTANCE.desensitize(json),
				equalTo("{\"username\":\"username\",\"password\":\"******\"}"));
		json = "{\"user\":{\"username\":\"username\",\"password\":\"password\"}}";
		assertThat(JsonDesensitizer.DEFAULT_INSTANCE.desensitize(json),
				equalTo("{\"user\":{\"username\":\"username\",\"password\":\"******\"}}"));
	}

	@Test
	public void testToJson() {
		assertThat(JsonDesensitizer.DEFAULT_INSTANCE.toJson(new User("username", "password", 12)).contains("******"),
				equalTo(true));
	}

	@Test
	public void testCustomize() {
		JsonDesensitizer desensitizer = new JsonDesensitizer();
		Map<BiPredicate<String, Object>, Function<String, String>> mapping = desensitizer.getMapping();
		mapping.clear();
		assertThat(desensitizer.toJson(new User("username", "password", 12)).contains("******"), equalTo(false));
		mapping.put((s, obj) -> s.equals("username") && obj instanceof User, s -> "------");
		mapping.put((s, obj) -> s.equals("age") && obj instanceof User, s -> "0.0");
		String json = desensitizer.toJson(new User("myname", "mypass", 12));
		assertThat(json.contains("------"), equalTo(true));
		assertThat(json.contains("\"mypass\""), equalTo(true));
		assertThat(json.contains("12"), equalTo(false));
		assertThat(json.contains("0.0"), equalTo(true));

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
