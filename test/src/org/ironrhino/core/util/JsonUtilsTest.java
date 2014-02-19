package org.ironrhino.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Lob;

import org.ironrhino.core.metadata.NotInJson;
import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;

public class JsonUtilsTest {

	static enum Status {
		ACTIVE, DISABLED;
		@Override
		public String toString() {
			return name().toLowerCase();
		}
	}

	static class User {
		private String username;
		@NotInJson
		private String password;
		private int age;
		private Status status;
		@Lob
		private String content;

		private Date date = DateUtils.beginOfDay(new Date());

		public Date getDate() {
			return date;
		}

		public void setDate(Date date) {
			this.date = date;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		public int getAge() {
			return age;
		}

		public void setAge(int age) {
			this.age = age;
		}

		public Status getStatus() {
			return status;
		}

		public void setStatus(Status status) {
			this.status = status;
		}

		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
		}

	}

	@Test
	public void testJson() {
		User u = new User();
		u.setUsername("username");
		u.setPassword("password");
		u.setStatus(Status.ACTIVE);
		u.setAge(12);
		u.setContent("this is a lob");
		String json = JsonUtils.toJson(u);
		try {
			User u2 = JsonUtils.fromJson(json, User.class);
			assertEquals(u.getUsername(), u2.getUsername());
			assertEquals(u.getAge(), u2.getAge());
			assertEquals(u.getStatus(), u2.getStatus());
			assertEquals(u.getDate().getTime(), u2.getDate().getTime());
			assertNull(u2.getPassword());
			assertNull(u2.getContent());
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testDate() {
		Date d = new Date();
		String json = "{\"date\":" + d.getTime() + "}";
		try {
			User u = JsonUtils.fromJson(json, User.class);
			assertEquals(d, u.getDate());
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testFromJsonUsingTypeReference() {
		List<User> users = new ArrayList<User>();
		for (int i = 0; i < 5; i++) {
			User u = new User();
			u.setUsername("username");
			u.setPassword("password");
			u.setStatus(Status.ACTIVE);
			u.setAge(12);
			users.add(u);
		}
		String json = JsonUtils.toJson(users);
		try {
			List<User> list = JsonUtils.fromJson(json,
					new TypeReference<List<User>>() {
					});
			assertEquals(users.size(), list.size());
			assertEquals(users.get(0).getUsername(), list.get(0).getUsername());
			assertEquals(users.get(0).getAge(), list.get(0).getAge());
			assertEquals(users.get(0).getStatus(), list.get(0).getStatus());
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}
