package org.ironrhino.core.util;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;

import org.hamcrest.Matcher;
import org.ironrhino.core.metadata.JsonDesensitize;
import org.junit.Test;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class JsonDesensitizerTest {

	private static final Matcher<String> containsMask = containsString("\"******\"");

	@Test
	public void testDesensitize() {
		JsonDesensitizer desensitizer = new JsonDesensitizer();
		String json = "{\"password\":\"password\"}";
		assertThat(desensitizer.desensitize(json), containsMask);
		json = "{\"username\":\"username\",\"password\":\"password\"}";
		assertThat(desensitizer.desensitize(json), containsMask);
		json = "{\"user\":{\"username\":\"username\",\"password\":\"password\"}}";
		assertThat(desensitizer.desensitize(json), containsMask);
		json = "{\"user\":{\"user2\":{\"username\":\"username\",\"password\":\"password\"}}}";
		assertThat(desensitizer.desensitize(json), containsMask);

		desensitizer.getDropping().add((name, parent) -> name.equals("username"));
		assertThat(desensitizer.desensitize(json), not(containsString("\"username\"")));
		desensitizer.getDropping().add((name, parent) -> name.equals("user2"));
		assertThat(desensitizer.desensitize(json), not(containsString("\"user2\"")));
		desensitizer.getDropping().add((name, parent) -> name.equals("user"));
		assertThat(desensitizer.desensitize(json), not(containsString("\"user\"")));
	}

	@Test
	public void testToJson() {
		JsonDesensitizer desensitizer = new JsonDesensitizer();
		User user = new User("username", "password", 12);
		User mate = new User("mate", "password", 11);
		user.mate = mate;
		assertThat(desensitizer.toJson(user), containsMask);
		assertThat(desensitizer.toJson(user), containsString("\"mate\""));
		assertThat(desensitizer.toJson(Collections.singletonMap("user", user)), containsString("\"user\""));

		desensitizer.getDropping().add((name, parent) -> name.equals("password"));
		assertThat(desensitizer.toJson(user), not(containsString("\"password\"")));
		desensitizer.getDropping().add((name, parent) -> name.equals("mate"));
		assertThat(desensitizer.toJson(user), not(containsString("\"mate\"")));
		desensitizer.getDropping().add((name, parent) -> name.equals("user"));
		assertThat(desensitizer.toJson(Collections.singletonMap("user", user)), not(containsString("\"user\"")));
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
		desensitizer.getDropping().add((s, obj) -> s.equals("age") && obj instanceof User);
		json = desensitizer.toJson(new User("myname", "mypass", 12));
		assertThat(json, containsString("------"));
		assertThat(json, containsString("\"mypass\""));
		assertThat(json, not(containsString("age")));
	}

	@Test
	public void testToJsonWithAnnotation() {
		JsonDesensitizer desensitizer = new JsonDesensitizer();
		Person p = new Person("test", "13333333333", "13333333333", "13333333333", "13333333333", "110199012120220",
				12);
		String json = desensitizer.toJson(p);
		assertThat(json, containsString("\"1**********\""));
		assertThat(json, containsString("\"****3333333\""));
		assertThat(json, containsString("\"133****3333\""));
		assertThat(json, containsString("\"1333333333****\""));
		assertThat(json, containsString("\"1101990121****0\""));
		assertThat(json, not(containsString("age")));
		assertThat(json, not(containsString("id_NO")));
	}

	@Test
	public void testDesensitizeValue() {
		JsonDesensitizer desensitizer = new JsonDesensitizer();
		JsonDesensitize config = new JsonDesensitize() {
			@Override
			public Class<? extends Annotation> annotationType() {
				return JsonDesensitize.class;
			}

			@Override
			public String value() {
				return "**";
			}

			@Override
			public int position() {
				return 2;
			}

		};
		assertThat(desensitizer.desensitizeValue("test", config), equalTo("\"te**\""));
		assertThat(desensitizer.desensitizeValue(new BigDecimal("12.3"), config), equalTo("\"**\""));

		String json = desensitizer.desensitizeValue(
				new Person("test", "13333333333", "13333333333", "13333333333", "13333333333", "110199012120220", 12),
				config);
		assertThat(json, containsString("\"1**********\""));
		assertThat(json, containsString("\"****3333333\""));
		assertThat(json, containsString("\"133****3333\""));
		assertThat(json, containsString("\"1333333333****\""));
		assertThat(json, not(containsString("age")));
	}

	@Test
	public void testDesensitizeArray() {
		JsonDesensitizer desensitizer = new JsonDesensitizer();
		assertThat(desensitizer.desensitizeArray(new Object[] { 1, "test", null }, null),
				equalTo("[ 1, \"test\", null ]"));

		String json = desensitizer
				.desensitizeArray(
						new Object[] { 1, "test", new Person("test", "13333333333", "13333333333", "13333333333",
								"13333333333", "110199012120220", 12) },
						new JsonDesensitize[] { null, new JsonDesensitize() {
							@Override
							public Class<? extends Annotation> annotationType() {
								return JsonDesensitize.class;
							}

							@Override
							public String value() {
								return "**";
							}

							@Override
							public int position() {
								return 2;
							}

						}, null });
		assertThat(json, containsString("\"te**\""));
		assertThat(json, containsString("\"1**********\""));
		assertThat(json, containsString("\"****3333333\""));
		assertThat(json, containsString("\"133****3333\""));
		assertThat(json, containsString("\"1333333333****\""));
		assertThat(json, not(containsString("age")));
	}

	@RequiredArgsConstructor
	@Getter
	static class User {
		private final String username;
		private final String password;
		private final int age;
		private User mate;
	}

	@RequiredArgsConstructor
	@Getter
	static class Person {
		private final String name;
		@JsonDesensitize("1**********")
		private final String phone1;
		@JsonDesensitize(value = "****", position = 0)
		private final String phone2;
		@JsonDesensitize(value = "****", position = 3)
		private final String phone3;
		@JsonDesensitize(value = "****", position = 10)
		private final String phone4;
		@JsonDesensitize(value = "****", position = 10)
		private final String ID_NO;
		@JsonDesensitize
		private final int age;
	}

}
