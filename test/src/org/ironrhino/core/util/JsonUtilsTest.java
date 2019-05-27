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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.persistence.Lob;

import org.ironrhino.common.model.Coordinate;
import org.ironrhino.core.metadata.View;
import org.ironrhino.core.model.BaseTreeableEntity;
import org.ironrhino.core.model.ResultPage;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

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

		@JsonSerialize(using = ToIdJsonSerializer.class)
		@JsonDeserialize(using = FromIdJsonDeserializer.class)
		@JsonView(View.Detail.class)
		private Department department;

		@JsonSerialize(using = ToIdJsonSerializer.class)
		@JsonDeserialize(using = FromIdJsonDeserializer.class)
		@JsonView(View.Detail.class)
		private List<Department> departments = new ArrayList<>();

		@JsonSerialize(using = ToIdJsonSerializer.class)
		@JsonDeserialize(using = FromIdJsonDeserializer.class)
		@JsonView(View.Detail.class)
		private Department[] depts;

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

	static class Department extends BaseTreeableEntity<Department> {

		private static final long serialVersionUID = 1L;

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

	@Data
	static class CoordinateObject {
		private Coordinate coordinate;
		private List<Coordinate> coordinates;
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

	@Test
	public void testToIdJsonSerializer() throws IOException {
		User u = new User();
		u.setUsername("username");
		u.setPassword("password");
		u.setStatus(Status.ACTIVE);
		u.setAge(12);
		u.setContent("this is a lob");
		Department department = new Department();
		department.setId(12L);
		Department department2 = new Department();
		department2.setId(13L);
		u.setDepartment(department);
		u.getDepartments().add(department);
		u.getDepartments().add(department2);
		u.setDepts(u.getDepartments().toArray(new Department[0]));
		String json = JsonUtils.toJson(u);
		JsonNode root = JsonUtils.fromJson(json, JsonNode.class);
		JsonNode node = root.get("department");
		assertThat(node.isNumber(), equalTo(true));
		assertThat(node.asLong(), equalTo(department.getId()));
		node = root.get("departments");
		assertThat(node.isArray(), equalTo(true));
		assertThat(node.get(0).asLong(), equalTo(department.getId()));
		assertThat(node.get(1).asLong(), equalTo(department2.getId()));
		node = root.get("depts");
		assertThat(node.isArray(), equalTo(true));
		assertThat(node.get(0).asLong(), equalTo(department.getId()));
		assertThat(node.get(1).asLong(), equalTo(department2.getId()));
		testFromIdJsonSerializer(json, department, department2);
		json = "{\"username\":\"username\",\"age\":12,\"status\":\"ACTIVE\",\"department\":{\"id\":12},\"departments\":[{\"id\":12},{\"id\":13}],\"depts\":[{\"id\":12},{\"id\":13}],\"date\":\"2019-05-24 00:00:00\"}";
		testFromIdJsonSerializer(json, department, department2);
	}

	private void testFromIdJsonSerializer(String json, Department department, Department department2)
			throws IOException {
		User user = JsonUtils.fromJson(json, User.class);
		assertThat(user.getDepartment().getId(), equalTo(department.getId()));
		assertThat(user.getDepartments().get(0).getId(), equalTo(department.getId()));
		assertThat(user.getDepartments().get(1).getId(), equalTo(department2.getId()));
		assertThat(user.getDepts()[0].getId(), equalTo(department.getId()));
		assertThat(user.getDepts()[1].getId(), equalTo(department2.getId()));
	}

	@Test
	public void testDeserializeCoordinate() throws IOException {
		Coordinate coordinate = new Coordinate(23.0, 113.0);
		CoordinateObject co = new CoordinateObject();
		co.setCoordinate(coordinate);
		co.setCoordinates(Collections.singletonList(coordinate));
		String json = JsonUtils.toJson(co);
		assertThat(JsonUtils.fromJson(json, CoordinateObject.class), equalTo(co));
		json = "{\"coordinate\":\"23.0,113.0\",\"coordinates\":[\"23.0,113.0\"]}";
		assertThat(JsonUtils.fromJson(json, CoordinateObject.class), equalTo(co));
		json = "{\"coordinate\":[23.0,113.0],\"coordinates\":[[23.0,113.0]]}";
		assertThat(JsonUtils.fromJson(json, CoordinateObject.class), equalTo(co));
		json = "{\"coordinate\":\"POINT (113.0 23.0)\",\"coordinates\":[\"POINT (113.0 23.0)\"]}";
		assertThat(JsonUtils.fromJson(json, CoordinateObject.class), equalTo(co));
	}
}
