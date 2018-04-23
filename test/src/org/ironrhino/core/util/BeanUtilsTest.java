package org.ironrhino.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ironrhino.core.metadata.NotInCopy;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.model.Treeable;
import org.ironrhino.core.spring.converter.CustomConversionService;
import org.junit.Test;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class BeanUtilsTest {

	public static @Data class Base implements Serializable, Persistable<String> {

		private static final long serialVersionUID = 1616212908678942555L;
		protected String id;
		private List<String> names;
		private Set<String> tags;
		private Map<String, String> attributes;

		@Override
		public boolean isNew() {
			return id == null;
		}

	}

	public static enum UserType {
		A, B
	}

	public static enum UserType2 {
		A, B
	}

	@Getter
	@Setter
	public static class User extends Base {

		private static final long serialVersionUID = -1634488900558289348L;
		private String username;
		@NotInCopy
		private String password;

		private Team team;

		private UserType type = UserType.A;

	}

	@Getter
	@Setter
	public static class User2 extends Base {

		private static final long serialVersionUID = 5095445311043690504L;
		private String username;
		@NotInCopy
		private String password;

		private UserType2 type = UserType2.A;

	}

	public static enum TeamType {
		A, B
	}

	@Getter
	@Setter
	public static class Team extends Base {

		private static final long serialVersionUID = 8360294456202382419L;

		private String name;

		private User owner;

		private TeamType type;

		private List<User> users;

		private Date createDate = new Date();

	}

	public static enum TeamType2 {
		A, B
	}

	@Getter
	@Setter
	public static class Team2 extends Base {

		private static final long serialVersionUID = 3455118342183020069L;

		private String name;

		private User2 owner;

		private TeamType2 type;

		private List<User2> users;

	}

	@Getter
	@Setter
	public static class Team3 extends Base {

		private static final long serialVersionUID = 1709649066766563947L;

		private String name;

		private String owner;

		private String createDate;

	}

	@Getter
	@Setter
	public static class Team4 extends Base {

		private static final long serialVersionUID = 4712258219610091729L;

		private String name;

		private User owner;

		private Date createDate;

		public Team4(Team team) {
			this.id = team.getId();
			this.name = team.getName();
			this.owner = team.getOwner();
			this.createDate = team.getCreateDate();
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

	@Getter
	@Setter
	@NoArgsConstructor
	public static class TreeA implements Treeable<TreeA> {
		private String name;
		private TreeA parent;
		private List<TreeA> children = new ArrayList<>();

		public TreeA(String name) {
			this.name = name;
		}
	}

	@Getter
	@Setter
	@NoArgsConstructor
	public static class TreeB implements Treeable<TreeB> {
		private String name;
		private TreeB parent;
		private List<TreeB> children = new ArrayList<>();

		public TreeB(String name) {
			this.name = name;
		}
	}

	@Test
	public void testCopyTree() {
		TreeA root = new TreeA("root");
		TreeA leaf1 = new TreeA("leaf1");
		leaf1.setParent(root);
		root.getChildren().add(leaf1);
		TreeA leaf2 = new TreeA("leaf2");
		leaf2.setParent(root);
		root.getChildren().add(leaf2);
		assertEquals(1, root.getLevel());
		assertEquals(2, leaf1.getLevel());
		assertEquals(2, leaf2.getLevel());
		TreeA rootCopy = BeanUtils.copyTree(root);
		assertEquals(1, rootCopy.getLevel());
		assertEquals(2, rootCopy.getChildren().size());
		assertEquals("leaf1", rootCopy.getChildren().get(0).getName());
		TreeB rootCopyB = BeanUtils.copyTree(root, TreeB::new);
		assertEquals(1, rootCopyB.getLevel());
		assertEquals(2, rootCopyB.getChildren().size());
		assertEquals("leaf1", rootCopyB.getChildren().get(0).getName());
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
	public void testCopyPropertiesIfNotNull() {
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
	public void testGetPropertyDescriptor() {
		assertNull(BeanUtils.getPropertyDescriptor(User.class, "none"));
		assertNull(BeanUtils.getPropertyDescriptor(User.class, "team.none"));
		assertNotNull(BeanUtils.getPropertyDescriptor(User.class, "team"));
		assertNotNull(BeanUtils.getPropertyDescriptor(User.class, "team.owner.id"));
	}

	@Test
	public void testSetPropertyValue() {
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
		u = new User();
		BeanUtils.setPropertyValue(u, "team.owner.username", "test");
		assertNotNull(u.getTeam());
		assertNotNull(u.getTeam().getOwner());
		assertEquals("test", u.getTeam().getOwner().getUsername());
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
		assertEquals(team.getOwner().getId(), team3.getOwner());
		assertNotNull(team3.getName());
		assertNotNull(team3.getCreateDate());

		Team4 team4 = CustomConversionService.getSharedInstance().convert(team, Team4.class);
		assertNotNull(team4);
		assertEquals(team1.getOwner(), team4.getOwner());
		assertEquals(team1.getCreateDate(), team4.getCreateDate());
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

	@Test
	public void testCreateParentIfNull() {
		User u = new User();
		assertNull(u.getAttributes());
		assertNull(u.getTeam());
		BeanUtils.createParentIfNull(u, "attributes");
		assertNull(u.getAttributes());
		BeanUtils.createParentIfNull(u, "attributes['test']");
		assertNotNull(u.getAttributes());
		BeanUtils.createParentIfNull(u, "team");
		assertNull(u.getTeam());
		BeanUtils.createParentIfNull(u, "team.owner");
		assertNotNull(u.getTeam());
		u = new User();
		Team t = new Team();
		u.setTeam(t);
		BeanUtils.createParentIfNull(u, "team.owner");
		BeanUtils.createParentIfNull(u, "team.owner.username");
		assertNotNull(u.getTeam());
		assertEquals(t, u.getTeam());
		assertNotNull(u.getTeam().getOwner());
		u = new User();
		BeanUtils.createParentIfNull(u, "team.users[0].username");
		assertNotNull(u.getTeam().getUsers());
	}

	@Test
	public void testIsEmpty() {
		Team3 team = new Team3();
		assertTrue(BeanUtils.isEmpty(team));
		team.setCreateDate("");
		assertTrue(!BeanUtils.isEmpty(team));
	}

}
