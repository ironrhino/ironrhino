package org.ironrhino.core.util;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

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

		private Team team;

		public Team getTeam() {
			return team;
		}

		public void setTeam(Team team) {
			this.team = team;
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

	}

	static enum TeamType {
		A, B
	}

	static class Team extends Base {
		private String name;

		private User owner;

		private TeamType type;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public User getOwner() {
			return owner;
		}

		public void setOwner(User owner) {
			this.owner = owner;
		}

		public TeamType getType() {
			return type;
		}

		public void setType(TeamType type) {
			this.type = type;
		}

	}

	static enum TeamType2 {
		A, B
	}

	static class Team2 extends Base {
		private String name;

		private User owner;

		private TeamType2 type;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public User getOwner() {
			return owner;
		}

		public void setOwner(User owner) {
			this.owner = owner;
		}

		public TeamType2 getType() {
			return type;
		}

		public void setType(TeamType2 type) {
			this.type = type;
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

	@Test
	public void copyPropertiesIfNotNull() {
		User user1 = new User();
		user1.setId("test");
		user1.setUsername("username");

		User user2 = new User();
		user2.setPassword("password");
		BeanUtils.copyPropertiesIfNotNull(user1, user2, "id", "username", "password");
		assertEquals(user2.getId(), "test");
		assertEquals(user2.getUsername(), "username");
		assertEquals(user2.getPassword(), "password");
	}

	@Test
	public void getPropertyDescriptor() {
		assertNull(BeanUtils.getPropertyDescriptor(User.class, "none"));
		assertNull(BeanUtils.getPropertyDescriptor(User.class, "team.none"));
		assertNotNull(BeanUtils.getPropertyDescriptor(User.class, "team"));
		assertNotNull(BeanUtils.getPropertyDescriptor(User.class, "team.owner.id"));
	}

	@Test
	public void setPropertyValue() {
		User u = new User();
		Team team = new Team();
		team.setName("test");
		assertNull(u.getTeam());
		BeanUtils.setPropertyValue(u, "team", team);
		assertNotNull(u.getTeam());
		u = new User();
		BeanUtils.setPropertyValue(u, "team.name", "test");
		assertNotNull(u.getTeam());
		assertEquals("test", u.getTeam().getName());
	}

	@Test
	public void testCopyEnumByName() {
		Team team = new Team();
		team.setName("name");
		team.setType(TeamType.A);
		Team team1 = new Team();
		BeanUtils.copyProperties(team, team1);
		assertEquals(team.getName(), team1.getName());
		assertEquals(team.getType().name(), team1.getType().name());
		Team2 team2 = new Team2();
		BeanUtils.copyProperties(team, team2);
		assertEquals(team.getName(), team2.getName());
		assertEquals(team.getType().name(), team2.getType().name());

	}

}
