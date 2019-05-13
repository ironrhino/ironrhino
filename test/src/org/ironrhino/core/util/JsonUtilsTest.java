package org.ironrhino.core.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.persistence.Lob;

import org.ironrhino.core.metadata.View;
import org.ironrhino.core.model.ResultPage;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;

public class JsonUtilsTest {

	static enum Status {
		ACTIVE, DISABLED;
		@Override
		public String toString() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	@Getter
	@Setter
	static class User {
		@JsonView(View.Summary.class)
		private String username;
		private String password;
		@JsonView(View.Detail.class)
		private int age;
		@JsonView(View.Detail.class)
		private Status status;
		@Lob
		private String content;

		@JsonView(View.Detail.class)
		private Date date = DateUtils.beginOfDay(new Date());

		@JsonIgnore
		public String getPassword() {
			return password;
		}

		@JsonProperty
		public void setPassword(String password) {
			this.password = password;
		}

	}

	@Data
	static class TemporalObject {
		private LocalDate date = LocalDate.now();
		private LocalDateTime datetime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
		private LocalTime time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
		private YearMonth month = YearMonth.now();
		private Duration duration = Duration.ofMillis(1000);
	}

	@Value
	static class ImmutableObject {
		private long id;
		private String name;
	}

	@Test
	public void testJson() throws IOException {
		User u = new User();
		u.setUsername("username");
		u.setPassword("password");
		u.setStatus(Status.ACTIVE);
		u.setAge(12);
		u.setContent("this is a lob");
		String json = JsonUtils.toJson(u);
		User u2 = JsonUtils.fromJson(json, User.class);
		assertThat(u.getUsername(), u2.getUsername(), equalTo(u.getUsername()));
		assertThat(u2.getAge(), equalTo(u.getAge()));
		assertThat(u2.getStatus(), equalTo(u.getStatus()));
		assertThat(u2.getDate().getTime(), equalTo(u.getDate().getTime()));
		assertThat(u2.getPassword(), nullValue());
		assertThat(u2.getContent(), nullValue());
	}

	@Test
	public void testJsonWithView() throws IOException {
		User u = new User();
		u.setUsername("username");
		u.setPassword("password");
		u.setStatus(Status.ACTIVE);
		u.setAge(12);
		u.setContent("this is a lob");
		String json = JsonUtils.toJsonWithView(u, View.Summary.class);
		JsonNode jsonNode = JsonUtils.fromJson(json, JsonNode.class);
		assertThat(jsonNode.size(), equalTo(1));
		jsonNode = jsonNode.get("username");
		assertThat(jsonNode.asText(), equalTo(u.getUsername()));
	}

	@Test
	public void testDate() throws IOException {
		Date d = new Date();
		String json = "{\"date\":" + d.getTime() + "}";
		User u = JsonUtils.fromJson(json, User.class);
		assertThat(u.getDate(), equalTo(d));

		json = "{\"date\":\"" + DateUtils.formatDate10(d) + "\"}";
		u = JsonUtils.fromJson(json, User.class);
		assertThat(u.getDate(), equalTo(DateUtils.beginOfDay(d)));

		json = "{\"date\":\"" + DateUtils.formatDatetime(d) + "\"}";
		u = JsonUtils.fromJson(json, User.class);
		assertThat(u.getDate().getTime() / 1000, equalTo(d.getTime() / 1000));
	}

	@Test
	public void testFromJsonUsingTypeReference() throws IOException {
		List<User> users = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			User u = new User();
			u.setUsername("username");
			u.setPassword("password");
			u.setStatus(Status.ACTIVE);
			u.setAge(12);
			users.add(u);
		}
		String json = JsonUtils.toJson(users);
		List<User> list = JsonUtils.fromJson(json, new TypeReference<List<User>>() {
		});
		assertThat(list.size(), equalTo(users.size()));
		assertThat(list.get(0).getUsername(), equalTo(users.get(0).getUsername()));
		assertThat(list.get(0).getAge(), equalTo(users.get(0).getAge()));
		assertThat(list.get(0).getStatus(), equalTo(users.get(0).getStatus()));
	}

	@Test
	public void testResultPage() throws IOException {
		ResultPage<User> rp = new ResultPage<>();
		List<User> users = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			User u = new User();
			u.setUsername("username");
			u.setPassword("password");
			u.setStatus(Status.ACTIVE);
			u.setAge(12);
			users.add(u);
		}
		rp.setResult(users);
		String json = JsonUtils.toJson(rp);
		ResultPage<User> rp2 = JsonUtils.fromJson(json, new TypeReference<ResultPage<User>>() {
		});
		assertThat(rp2.getResult().size(), equalTo(rp.getResult().size()));
		assertThat(rp2.getResult().iterator().next(), isA(User.class));
		String json2 = JsonUtils.toJson(rp2);
		assertThat(json2, equalTo(json));
	}

	@Test
	public void testTemporal() throws IOException {
		TemporalObject to = new TemporalObject();
		String s = JsonUtils.toJson(to);
		TemporalObject to2 = JsonUtils.fromJson(s, TemporalObject.class);
		assertThat(to2, equalTo(to));
	}

	@Test
	public void testImmutable() throws IOException {
		assertThat(JsonUtils.fromJson("{\"id\":12,\"name\":\"test\"}", ImmutableObject.class),
				equalTo(new ImmutableObject(12, "test")));
	}

}
