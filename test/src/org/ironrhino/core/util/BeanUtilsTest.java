package org.ironrhino.core.util;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.ironrhino.core.metadata.NotInCopy;
import org.junit.Test;

public class BeanUtilsTest {

	static class Base {

		protected String id;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

	}

	static class User extends Base {
		private String username;
		@NotInCopy
		private String password;

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

	}

	@Test
	public void hasProperty() {
		assertTrue(BeanUtils.hasProperty(User.class, "id"));
		assertTrue(BeanUtils.hasProperty(User.class, "username"));
		assertTrue(!BeanUtils.hasProperty(User.class, "test"));
	}

	@Test
	public void copyProperties() {
		User user1 = new User();
		user1.setId("test");
		user1.setUsername("username");
		user1.setPassword("password");

		User user2 = new User();
		BeanUtils.copyProperties(user1, user2);
		assertNotNull(user2.getId());
		assertNotNull(user2.getUsername());
		assertNull(user2.getPassword());

		user2 = new User();
		BeanUtils.copyProperties(user1, user2, "id");
		assertNull(user2.getId());
		assertNotNull(user2.getUsername());
		assertNull(user2.getPassword());

		user2 = new User();
		BeanUtils.copyProperties(user1, user2, "id", "username");
		assertNull(user2.getId());
		assertNull(user2.getUsername());
		assertNull(user2.getPassword());

	}

}
