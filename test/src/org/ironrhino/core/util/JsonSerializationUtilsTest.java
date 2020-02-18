package org.ironrhino.core.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Locale;

import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

public class JsonSerializationUtilsTest {

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
		private String username;
		private String password;
		private int age;
		private Status status;
		private String content;
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

	@Data
	static class ImmutableObject {
		private final long id;
		private final String name;
	}

	@Test
	public void testJson() throws IOException {
		User u = new User();
		u.setUsername("username");
		u.setPassword("password");
		u.setStatus(Status.ACTIVE);
		u.setAge(12);
		u.setContent("this is a lob");
		String json = JsonSerializationUtils.serialize(u);
		User u2 = (User) JsonSerializationUtils.deserialize(json);
		assertThat(u.getUsername(), u2.getUsername(), equalTo(u.getUsername()));
		assertThat(u2.getAge(), equalTo(u.getAge()));
		assertThat(u2.getStatus(), equalTo(u.getStatus()));
		assertThat(u2.getDate().getTime(), equalTo(u.getDate().getTime()));
		assertThat(u2.getPassword(), equalTo(u.getPassword()));
		assertThat(u2.getContent(), equalTo(u.getContent()));
	}

	@Test
	public void testTemporal() throws IOException {
		TemporalObject object = new TemporalObject();
		String json = JsonSerializationUtils.serialize(object);
		TemporalObject to2 = (TemporalObject) JsonSerializationUtils.deserialize(json);
		assertThat(to2, equalTo(object));
	}

	@Test
	public void testImmutable() throws IOException {
		ImmutableObject object = new ImmutableObject(12, "test");
		String json = JsonSerializationUtils.serialize(object);
		assertThat(JsonSerializationUtils.deserialize(json), equalTo(object));
	}

}
