package org.ironrhino.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ironrhino.core.metadata.NotInCopy;
import org.junit.Test;

public class BeanUtilsTest {

	public static class Base implements Serializable {

		private static final long serialVersionUID = 1616212908678942555L;
		protected String id;
		private List<String> names;
		private Set<String> tags;
		private Map<String, String> attributes;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public List<String> getNames() {
			return names;
		}

		public void setNames(List<String> names) {
			this.names = names;
		}

		public Set<String> getTags() {
			return tags;
		}

		public void setTags(Set<String> tags) {
			this.tags = tags;
		}

		public Map<String, String> getAttributes() {
			return attributes;
		}

		public void setAttributes(Map<String, String> attributes) {
			this.attributes = attributes;
		}

	}

	public static enum UserType {
		A, B
	}

	public static enum UserType2 {
		A, B
	}

	public static class User extends Base {

		private static final long serialVersionUID = -1634488900558289348L;
		private String username;
		@NotInCopy
		private String password;

		private Team team;

		private UserType type = UserType.A;

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

		public UserType getType() {
			return type;
		}

		public void setType(UserType type) {
			this.type = type;
		}

	}

	public static class User2 extends Base {

		private static final long serialVersionUID = 5095445311043690504L;
		private String username;
		@NotInCopy
		private String password;

		private UserType2 type = UserType2.A;

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

		public UserType2 getType() {
			return type;
		}

		public void setType(UserType2 type) {
			this.type = type;
		}

	}

	public static enum TeamType {
		A, B
	}

	public static class Team extends Base {

		private static final long serialVersionUID = 8360294456202382419L;

		private String name;

		private User owner;

		private TeamType type;

		private List<User> users;

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

		public List<User> getUsers() {
			return users;
		}

		public void setUsers(List<User> users) {
			this.users = users;
		}

	}

	public static enum TeamType2 {
		A, B
	}

	public static class Team2 extends Base {

		private static final long serialVersionUID = 3455118342183020069L;

		private String name;

		private User2 owner;

		private TeamType2 type;

		private List<User2> users;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public User2 getOwner() {
			return owner;
		}

		public void setOwner(User2 owner) {
			this.owner = owner;
		}

		public TeamType2 getType() {
			return type;
		}

		public void setType(TeamType2 type) {
			this.type = type;
		}

		public List<User2> getUsers() {
			return users;
		}

		public void setUsers(List<User2> users) {
			this.users = users;
		}

	}

	public static class Team3 extends Base {

		private static final long serialVersionUID = 1709649066766563947L;

		private String name;

		private String owner;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getOwner() {
			return owner;
		}

		public void setOwner(String owner) {
			this.owner = owner;
		}

	}

	private static class MyList<E> extends ArrayList<E> {
		private static final long serialVersionUID = 1L;
	}

	private static class MySet<E> extends HashSet<E> {
		private static final long serialVersionUID = 1L;
	}

	private static class MyMap<K, V> extends HashMap<K, V> {
		private static final long serialVersionUID = 1L;
	}

	@Test
	public void testHasProperty() {
		assertTrue(BeanUtils.hasProperty(User.class, "id"));
		assertTrue(BeanUtils.hasProperty(User.class, "username"));
		assertTrue(!BeanUtils.hasProperty(User.class, "test"));
	}

	@Test
	public void testCopyProperties() {
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

		User user3 = new User();
		BeanUtils.copyProperties(user1, user3, false);
		assertNotNull(user3.getId());
		assertNotNull(user3.getUsername());
		assertNotNull(user3.getPassword());

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

	@Test
	public void testSerializableToSerializable() {
		Team team = new Team();
		User user = new User();
		user.setId("id");
		user.setUsername("username");
		team.setName("name");
		team.setOwner(user);
		team.setUsers(Collections.singletonList(user));
		
		Team team1 = new Team();
		BeanUtils.copyProperties(team, team1);
		assertNotNull(team1.getOwner());
		assertEquals(1, team1.getUsers().size());
		assertEquals("username", team1.getUsers().get(0).getUsername());
		assertEquals("A", team1.getUsers().get(0).getType().name());

		Team2 team2 = new Team2();
		BeanUtils.copyProperties(team, team2);
		assertNotNull(team2.getOwner());
		assertEquals(1, team2.getUsers().size());
		assertEquals("username", team2.getUsers().get(0).getUsername());
		assertEquals("A", team2.getUsers().get(0).getType().name());
		assertEquals(User2.class, team2.getUsers().get(0).getClass());
		
		Team3 team3 = new Team3();
		BeanUtils.copyProperties(team, team3);
		assertEquals(team.getOwner().getId(),team3.getOwner());
	}

	@Test
	public void testNormalizeCollectionFields() {
		User user = new User();
		user.setId("id");
		user.setUsername("username");
		BeanUtils.normalizeCollectionFields(user);
		assertNull(user.getNames());
		assertNull(user.getTags());
		assertNull(user.getAttributes());
		List<String> names = new ArrayList<>();
		names.add("abc");
		Set<String> tags = new HashSet<>();
		tags.add("test");
		Map<String, String> attributes = new HashMap<>();
		attributes.put("test", "test");
		user.setNames(names);
		user.setTags(tags);
		user.setAttributes(attributes);
		BeanUtils.normalizeCollectionFields(user);
		assertTrue(user.getNames() == names);
		assertTrue(user.getTags() == tags);
		assertTrue(user.getAttributes() == attributes);

		List<String> names2 = new MyList<>();
		names2.addAll(names);
		Set<String> tags2 = new MySet<>();
		tags2.addAll(tags);
		Map<String, String> attributes2 = new MyMap<>();
		attributes2.putAll(attributes);
		user.setNames(names2);
		user.setTags(tags2);
		user.setAttributes(attributes2);
		assertTrue(user.getNames() == names2);
		assertTrue(user.getTags() == tags2);
		assertTrue(user.getAttributes() == attributes2);
		BeanUtils.normalizeCollectionFields(user);
		assertFalse(user.getNames() == names2);
		assertFalse(user.getTags() == tags2);
		assertFalse(user.getAttributes() == attributes2);
		assertEquals(user.getNames(), names);
		assertEquals(user.getTags(), tags);
		assertEquals(user.getAttributes(), attributes);
	}

}
